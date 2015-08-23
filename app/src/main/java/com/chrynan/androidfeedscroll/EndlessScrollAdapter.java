package com.chrynan.androidfeedscroll;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.chrynan.androidfeedscroll.cache.EndlessScrollCache;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chrynan on 8/22/2015.
 */
public class EndlessScrollAdapter extends RecyclerView.Adapter<EndlessScrollAdapter.ViewHolder> {
    //Base URL for loading the items, ex: https://example.com/rs/feed/getFeed/
    //When loading bottom items, will append userId/token/bottomLastLoadedItemId/amountOfItemsToLoad to the URL
    //When loading top items, will append userId/token/topLastLoadedItemId/ to the URL
    private String loadTopRestURL = "";
    private String loadBottomRestURL = "";
    private RecyclerView recyclerView;

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
        this.cache = new EndlessScrollCache(context);
    }

    public EndlessScrollAdapter(Context context, List<JSONObject> items, String userId, String token){
        this.context = context;
        this.userId = userId;
        this.token = token;
        this.items = items;
        this.cache = new EndlessScrollCache(context);
    }

    @Override
    public EndlessScrollAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //assumes the parent ViewGroup is the RecyclerView
        if(recyclerView == null){
            recyclerView = (RecyclerView) parent;
            EndlessRecyclerOnScrollListener scrollListener = new EndlessRecyclerOnScrollListener((LinearLayoutManager) recyclerView.getLayoutManager());
            recyclerView.setOnScrollListener(scrollListener);//deprecated should be addOnScrollListener for newer Android versions
        }
        return null;
    }

    @Override
    public void onBindViewHolder(EndlessScrollAdapter.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
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
        this.cache.cleanSave(this.items);
        notifyDataSetChanged();
    }

    public void addToBottom(JSONObject obj){
        this.items.add(obj);
        this.cache.save(obj);
        notifyDataSetChanged();
    }

    public void addToBottom(List<JSONObject> items){
        this.items.addAll(items);
        this.cache.save(items);
        notifyDataSetChanged();
    }
    /** End of adding and removing items **/


    public void loadMoreTop(final OnLoadListener listener){
        //TODO
    }

    public void loadMoreBottom(final OnLoadListener listener){
        //TODO
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
                    public void onCacheLoad(List<JSONObject> items) {}
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
