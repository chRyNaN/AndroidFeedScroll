package com.chrynan.androidfeedscroll;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.chrynan.androidfeedscroll.cache.EndlessScrollCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by chrynan on 8/22/2015. TODO better use generics and enable cache
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
    private EndlessRecyclerOnScrollListener scrollListener;
    public static final int DEFAULT_VIEW_TYPE = 435;
    private String defaultString;

    private Context context;
    //Application user credentials
    private String userId;
    private String token;
    private List<JSONObject> items; //The actual items loaded to be displayed
    private List<JSONObject> loadedItems; //items that were loaded; this allows you to display items that aren't considered when loading more
    private EndlessScrollCache cache;

    private Random random;

    public EndlessScrollAdapter(Context context, String userId, String token){
        this.context = context;
        this.userId = userId;
        this.token = token;
        this.items = new ArrayList<>();
        this.loadedItems = new ArrayList<>();
        this.cache = new EndlessScrollCache(context);
        random = new Random();
    }

    public EndlessScrollAdapter(Context context, List<JSONObject> items, String userId, String token){
        this.context = context;
        this.userId = userId;
        this.token = token;
        this.items = items;
        this.loadedItems = new ArrayList<>();
        this.cache = new EndlessScrollCache(context);
        random = new Random();
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
        dataChanged();
    }

    protected void addLoadedToTop(List<JSONObject> list){
        List<JSONObject> newList = new ArrayList<>();
        newList.addAll(list);
        newList.addAll(this.items);
        this.items = newList;
        newList = new ArrayList<>();
        newList.addAll(list);
        newList.addAll(this.loadedItems);
        this.loadedItems = newList;
        dataChanged();
    }

    public void addToBottom(JSONObject obj){
        this.items.add(obj);
        dataChanged();
    }

    public void addToBottom(List<JSONObject> list){
        this.items.addAll(list);
        dataChanged();
    }

    public int addToRandomPosition(JSONObject obj, int startPosition){
        int size = this.items.size();
        startPosition = (startPosition < 0) ? 0 : startPosition;
        startPosition = (startPosition >= size) ? size - 1 : startPosition;
        int position = startPosition;
        if(size < 1){
            position = 0;
            addToBottom(obj);
        }else{
            position = random.nextInt(size - startPosition) + startPosition;
            List<JSONObject> before = subList(this.items, 0, position + 1);
            List<JSONObject> after = null;
            if(position != size - 1){
                after = subList(this.items, position + 1, size);
            }
            this.items.clear();
            if(position != 0){
                this.items.addAll(before);
                this.items.add(obj);
            }else{
                this.items.add(obj);
                this.items.addAll(before);
            }
            if(after != null){
                this.items.addAll(after);
            }
        }
        dataChanged();
        return position;
    }

    public int addToRandomPosition(JSONObject obj){
        int size = this.items.size();
        int position = 0;
        if(size < 1){
            addToBottom(obj);
        }else{
            random = new Random();
            position = random.nextInt(size);
            List<JSONObject> before = subList(this.items, 0, position + 1);
            List<JSONObject> after = null;
            if(position != size - 1){
                after = subList(this.items, position + 1, size);
            }
            this.items.clear();
            if(position != 0) {
                this.items.addAll(before);
                this.items.add(obj);
            }else {
                this.items.add(obj);
                this.items.addAll(before);
            }
            if(after != null){
                this.items.addAll(after);
            }
        }
        dataChanged();
        return position;
    }

    private List<JSONObject> subList(List<JSONObject> list, int startIndex, int endIndexPlusOne){
        //The reason for creating a custom method for doing this instead of using ArrayList's subList() method is because
        //ArrayList's subList() method returns a List where alterations are reflected by the parent list and vice versa
        //making it difficult to perform the task I need. startIndex is inclusive; default is 0. endIndexPlusOne is exclusive.
        if(list == null || list.size() < 1){
            return null;
        }
        startIndex = (startIndex < 0) ? 0 : startIndex;
        startIndex = (startIndex > list.size() - 1) ? list.size() - 1 : startIndex;
        endIndexPlusOne = (endIndexPlusOne > list.size()) ? list.size() : endIndexPlusOne;
        endIndexPlusOne = (endIndexPlusOne < 1) ? 1 : endIndexPlusOne;
        List<JSONObject> result = new ArrayList<>();
        if(endIndexPlusOne <= startIndex){
            result.add(list.get(startIndex));
            return result;
        }
        for(int i = startIndex; i < endIndexPlusOne; i++){
            result.add(list.get(i));
        }
        return result;
    }

    protected boolean addLoadedToBottom(JSONObject obj){
        boolean add = true;
        for(JSONObject o : this.items){
            if(getObjectId(o).equals(getObjectId(obj))){
                add = false;
                break;
            }
        }
        if(add) {
            this.items.add(obj);
            this.loadedItems.add(obj);
            //this.cache.save(obj);
            dataChanged();
        }
        return add;
    }

    protected boolean addLoadedToBottom(List<JSONObject> items){
        boolean add = false;
        if(items.size() > this.items.size()) {
            add = true;
        }else {
            String comp1 = getObjectId(items.get(0));
            String comp2 = getObjectId(this.loadedItems.get(this.loadedItems.size() - (items.size() + 1)));
            if (comp1.equals(comp2)) {
                add = false;
            }else{
                add = true;
            }
        }
        if(add) {
            this.items.addAll(items);
            this.loadedItems.addAll(items);
            //this.cache.save(items);
            dataChanged();
        }
        return add;
    }

    public void remove(JSONObject obj){
        this.items.remove(obj);
        this.loadedItems.remove(obj);
        dataChanged();
    }

    public void clear(){
        this.items.clear();
        this.loadedItems.clear();
        dataChanged();
    }

    protected void dataChanged(){
        //this method will handle notifying that the data set has changed even if not called from the UI thread, as long as,
        //the context used when instantiating this class is an instance of Activity
        try {
            if(context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            } else {
                //try to call notifyDataSetChanged() on current thread, but it might throw an exception if it's not on the UI thread
                notifyDataSetChanged();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /** End of adding and removing items **/


    public void dismount(){
        //This method is called from the parent Activity or Fragment when it's onStop() or onDestroy() methods are called.
        //It's used for any clean ups that need to be done in the adapter.
    }

    public void loadMoreTop(final OnLoadListener listener){
        final String id = (this.loadedItems == null || this.loadedItems.size() < 1) ? "-1" : getObjectId(this.loadedItems.get(0));

        //the task to load data from the server
        final HttpTask t = new HttpTask() {
            @Override
            protected void onPostExecute(String result) {
                //result = {response: [], responseCode}
                //the actual response should be in the form of an array of JSONObjects
                try{
                    if(result != null && result.length() >= 1) {
                        JSONObject obj = new JSONObject(result);
                        JSONArray array = new JSONArray(obj.getString("response"));
                        List<JSONObject> list = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            list.add(array.getJSONObject(i));
                        }
                        //to avoid duplicate objects, for now we'll do a clean save and rewrite the items in the list
                        //should add better functionality later
                        items = list;
                        loadedItems = list;
                        if (scrollListener != null) {
                            scrollListener.restart();
                        }
                        //cache.cleanSave(list);
                        notifyDataSetChanged();
                        if (listener != null) {
                            listener.onLoad(list);
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    if(listener != null){
                        listener.onLoad(null);
                    }
                }
                onAfterLoad();
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
        final String id = (loadedItems == null || loadedItems.size() < 1) ? "-1" : getObjectId(loadedItems.get(loadedItems.size() - 1));
        //the task to load data from the server
        final HttpTask t = new HttpTask() {
            @Override
            protected void onPostExecute(String result) {
                //result = {response: [], responseCode}
                //the actual response should be in the form of an array of JSONObjects
                try {
                    if(result != null && result.length() >= 1) {
                        JSONObject obj = new JSONObject(result);
                        JSONArray array = new JSONArray(obj.getString("response"));
                        List<JSONObject> list = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            list.add(array.getJSONObject(i));
                        }
                        addLoadedToBottom(list);
                        if (listener != null) {
                            listener.onLoad(list);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if(listener != null){
                        listener.onLoad(null);
                    }
                }
                onAfterLoad();
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

    public void onAfterLoad(){}

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

    public String getDefaultString() {
        return defaultString;
    }

    public void setDefaultString(String defaultString) {
        this.defaultString = defaultString;
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
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
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
