package com.chrynan.androidfeedscroll;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.chrynan.androidfeedscroll.cache.EndlessScrollCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chrynan on 8/22/2015. TODO better use generics
 */
public class EndlessScrollAdapter extends RecyclerView.Adapter<EndlessScrollAdapter.ViewHolder> {
    //Base URL for loading the items, ex: https://example.com/rs/feed/getFeed/
    //When loading bottom items, will append userId/token/bottomLastLoadedItemId/amountOfItemsToLoad to the URL
    //When loading top items, will append userId/token/topLastLoadedItemId/ to the URL
    private String loadTopRestURL = "";
    private String loadBottomRestURL = "";
    private String objectIdField;
    private int defaultRetrieveAmount = 5;
    private RecyclerView recyclerView;
    EndlessRecyclerOnScrollListener scrollListener;
    public static final int DEFAULT_VIEW_TYPE = 435;

    private Context context;
    //Application user credentials
    private String userId;
    private String token;
    private List<JSONObject> items; //The actual items loaded to be displayed
    private EndlessScrollCache cache;

    public EndlessScrollAdapter(Context context, String userId, String token){
        this.context = context;
        this.userId = userId;
        this.token = token;
        this.items = new ArrayList<>();
        this.cache = new EndlessScrollCache(context);
    }

    public EndlessScrollAdapter(Context context, List<JSONObject> items, String userId, String token){
        this.context = context;
        this.userId = userId;
        this.token = token;
        this.items = items;
        this.cache = new EndlessScrollCache(context);
    }

    public void init(){
        //load the first set of items
        loadMoreTop(new OnLoadListener() {
            @Override
            public void onCacheLoad(List<JSONObject> items) {}
            @Override
            public void onLoad(List<JSONObject> items) {}
        });
    }

    @Override
    public EndlessScrollAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //assumes the parent ViewGroup is the RecyclerView
        if(recyclerView == null){
            recyclerView = (RecyclerView) parent;
            scrollListener = new EndlessRecyclerOnScrollListener((LinearLayoutManager) recyclerView.getLayoutManager());
            recyclerView.setOnScrollListener(scrollListener);//deprecated should be addOnScrollListener for newer Android versions
        }
        return null;
    }

    @Override
    public void onBindViewHolder(EndlessScrollAdapter.ViewHolder holder, int position) {}

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    public JSONObject getItem(int position){
        return this.items.get(position);
    }

    public List<JSONObject> getItems(){
        return this.items;
    }

    /** Adding and Removing items in the list **/
    public void addToTop(JSONObject obj){
        List<JSONObject> items = new ArrayList<>();
        items.add(obj);
        addToTop(items);
    }

    public void addToTop(List<JSONObject> items){
        List<JSONObject> newList = new ArrayList<>();
        newList.addAll(items);
        newList.addAll(this.items);
        this.items = newList;
        //this.cache.cleanSave(this.items);
        notifyDataSetChanged();
    }

    public void addToBottom(JSONObject obj){
        boolean add = true;
        for(JSONObject o : this.items){
            if(getObjectId(o).equals(getObjectId(obj))){
                add = false;
                break;
            }
        }
        if(add) {
            this.items.add(obj);
            //this.cache.save(obj);
            notifyDataSetChanged();
        }
    }

    public void addToBottom(List<JSONObject> items){
        boolean add = false;
        if(items.size() > this.items.size()) {
            add = true;
        }else {
            String comp1 = getObjectId(items.get(0));
            Log.d("APP", "this.items.size() = " + this.items.size() + " items.size() = " + items.size() + " " +
                    "this.items.size() - (items.size() + 1) = " + (this.items.size() - (items.size() + 1)));
            String comp2 = getObjectId(this.items.get(this.items.size() - (items.size() + 1)));
            if (comp1.equals(comp2)) {
                add = false;
            }else{
                add = true;
            }
        }
        Log.d("App", "ADDTOBOTTOM = " + add);
        if(add) {
            this.items.addAll(items);
            //this.cache.save(items);
            notifyDataSetChanged();
        }
    }

    public void remove(JSONObject obj){
        this.items.remove(obj);
    }

    public void clear(){
        this.items.clear();
    }
    /** End of adding and removing items **/


    public void loadMoreTop(final OnLoadListener listener){
        final String id = (this.items == null || this.items.size() < 1) ? "0" : getObjectId(this.items.get(0));

        //the task to load data from the server
        final HttpTask t = new HttpTask() {
            @Override
            protected void onPostExecute(String result) {
                //result = {response: [], responseCode}
                //the actual response should be in the form of an array of JSONObjects
                try{
                    Log.d("APP", "RESULT = " + result);
                    JSONObject obj = new JSONObject(result);
                    JSONArray array = new JSONArray(obj.getString("response"));
                    List<JSONObject> list = new ArrayList<>();
                    for(int i = 0; i < array.length(); i++){
                        list.add(array.getJSONObject(i));
                    }
                    //to avoid duplicate objects, for now we'll do a clean save and rewrite the items in the list
                    //should add better functionality later
                    items = list;
                    if(scrollListener != null){
                        scrollListener.restart();
                    }
                    //cache.cleanSave(list);
                    notifyDataSetChanged();
                    listener.onLoad(list);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        //first check the cache for items to display while we load items from the server
        if(cache.exists()){/*
            cache.read(new EndlessScrollCache.OnReadCacheListener() {
                @Override
                public void onRead(List<JSONObject> results) {
                    items = results;
                    notifyDataSetChanged();
                    listener.onCacheLoad(results);*/
                    t.execute(loadTopRestURL + userId + "/" + token + "/" + id);
                //}
            //});
        }else{
            t.execute(loadTopRestURL + userId + "/" + token + "/" + id);
        }
    }

    public void loadMoreBottom(final OnLoadListener listener){
        Log.d("APP", "loadMoreBottom");
        final String id = getObjectId(items.get(items.size() - 1));
        //the task to load data from the server
        final HttpTask t = new HttpTask() {
            @Override
            protected void onPostExecute(String result) {
                //result = {response: [], responseCode}
                //the actual response should be in the form of an array of JSONObjects
                try{
                    JSONObject obj = new JSONObject(result);
                    JSONArray array = new JSONArray(obj.getString("response"));
                    List<JSONObject> list = new ArrayList<>();
                    for(int i = 0; i < array.length(); i++){
                        list.add(array.getJSONObject(i));
                    }
                    addToBottom(list);
                    listener.onLoad(list);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        //first check the cache for items to display while we load items from the server
        if(cache.exists()){/*
            cache.read(new EndlessScrollCache.OnReadCacheListener() {
                @Override
                public void onRead(List<JSONObject> results) {
                    addToBottom(results);
                    listener.onCacheLoad(results);*/
                    t.execute(loadBottomRestURL + userId + "/" + token + "/" + id + "/" + defaultRetrieveAmount);
                //}
            //});
        }else{
            t.execute(loadBottomRestURL + userId + "/" + token + "/" + id + "/" + defaultRetrieveAmount);
        }
    }

    public String getObjectId(JSONObject obj){
        if (this.items != null && this.items.size() >= 1) {
            //trying to be ambiguous as possible to allow for customization, however, we need a way of identifying items,
            //for now, just assuming there's an "id" field.
            try {
                if (objectIdField != null && obj.has(objectIdField)) {
                    return obj.getString(objectIdField);
                } else if (obj.has("id")) {
                    return obj.getString("id");
                }
            }catch(JSONException j){
                j.printStackTrace();
            }
        }
        return "";
    }

    public String getLoadBottomRestURL() {
        return loadBottomRestURL;
    }

    public void setLoadBottomRestURL(String loadBottomRestURL) {
        this.loadBottomRestURL = loadBottomRestURL;
    }

    public String getLoadTopRestURL() {
        return loadTopRestURL;
    }

    public void setLoadTopRestURL(String loadTopRestURL) {
        this.loadTopRestURL = loadTopRestURL;
    }

    public String getObjectIdField() {
        return objectIdField;
    }

    public void setObjectIdField(String objectIdField) {
        this.objectIdField = objectIdField;
    }

    public int getDefaultRetrieveAmount() {
        return defaultRetrieveAmount;
    }

    public void setDefaultRetrieveAmount(int defaultRetrieveAmount) {
        this.defaultRetrieveAmount = defaultRetrieveAmount;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(View v){
            super(v);
        }
    }


    public interface OnLoadListener{
        void onCacheLoad(List<JSONObject> items);
        void onLoad(List<JSONObject> items);
    }


    public static class EndlessRecyclerOnScrollListener extends RecyclerView.OnScrollListener{
        private int previousTotal = 0; // The total number of items in the dataset after the last load
        private boolean loading = true; // True if we are still waiting for the last set of data to load.
        private int visibleThreshold = 5; // The amount of items to have below your current scroll position before loading more.
        int firstVisibleItem, visibleItemCount, totalItemCount;
        private int currentPage = 1;
        private LinearLayoutManager mLinearLayoutManager;

        public EndlessRecyclerOnScrollListener(LinearLayoutManager layoutManager){
            super();
            mLinearLayoutManager = layoutManager;
        }

        public EndlessRecyclerOnScrollListener(LinearLayoutManager layoutManager, int threshold){
            super();
            mLinearLayoutManager = layoutManager;
            visibleThreshold = threshold;
        }

        public void restart(){
            previousTotal = 0;
            loading = true;
        }

        @Override
        public void onScrolled(RecyclerView view, int dx, int dy){
            super.onScrolled(view, dx, dy);

            visibleItemCount = view.getChildCount();
            totalItemCount = mLinearLayoutManager.getItemCount();
            firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();
            Log.d("onScrolled", "totalItemCount = " + totalItemCount + " visibleItemCount = " + visibleItemCount +
                    " firstVisibleItem = " + firstVisibleItem + " visibleThreshold = " + visibleThreshold + " previousTotal = " + previousTotal);
            Log.d("onScrolled", "LOADING = " + loading);
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            Log.d("onScrolled", "(totalItemCount - visibileItemCount) = " + (totalItemCount - visibleItemCount) +
                    " firstVisibileItem + visiblieThreshold = " + (firstVisibleItem + visibleThreshold));
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                Log.d("onScrolled", "load more");
                // End has been reached
                // Do something
                ((EndlessScrollAdapter) view.getAdapter()).loadMoreBottom(new OnLoadListener() {
                    @Override
                    public void onCacheLoad(List<JSONObject> items) {
                    }

                    @Override
                    public void onLoad(List<JSONObject> items) {
                        currentPage++;
                    }
                });
                loading = true;
            }
        }

    }//End of EndlessRecyclerOnScrollListener

}//End of EndlessScrollAdapter
