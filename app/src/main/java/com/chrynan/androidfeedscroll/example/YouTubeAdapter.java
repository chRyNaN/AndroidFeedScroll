package com.chrynan.androidfeedscroll.example;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chrynan.androidfeedscroll.EndlessScrollAdapter;
import com.chrynan.androidfeedscroll.R;
import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by byowa_000 on 8/26/2015.
 * Beware of known error with the YouTubeAndroidPlayerAPI, located here:
 * https://code.google.com/p/gdata-issues/issues/detail?id=7533&q=uncatchable&colspec=API%20ID%20Type%20Status%20Priority%20Stars%20Summary
 */
public class YouTubeAdapter extends EndlessScrollAdapter {
    private static final int YOUTUBE_VIEW_TYPE = 268;
    private static final long NUMBER_OF_VIDEOS_RETURNED = 5;
    private boolean supportsYouTube = false;
    private boolean initialized = false;
    private Context context;
    private Map<String, YouTubeThumbnailLoader> loaders;
    private Map<String, YouTubeThumbnailView> initializing;
    private static YouTube youtube;
    private String youTubeId;
    private String applicationName;
    //for obtaining search query; info to look for
    private JSONArray searchInfo;
    private int lastAddedPosition;

    public YouTubeAdapter(Context context, String userId, String token) {
        super(context, userId, token);
        this.context = context;
        this.loaders = new HashMap<>();
        this.initializing = new HashMap<>();
        youTubeId = context.getString(R.string.google_api_developer_key);
        applicationName = context.getString(R.string.app_name);
        lastAddedPosition = 0;
        //Check if user has the YouTube app installed
        final YouTubeInitializationResult result = YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(context);
        if (result != YouTubeInitializationResult.SUCCESS) {
            supportsYouTube = false;
        }else{
            supportsYouTube = true;
            youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                @Override
                public void initialize(com.google.api.client.http.HttpRequest request) throws IOException {}
            }).setApplicationName(getApplicationName()).build();
        }
    }

    public void initYouTube(JSONArray searchInfo){
        initialized = true;
        this.searchInfo = searchInfo;
    }

    public void update(JSONArray searchInfo){
        //updating the search criteria
        this.searchInfo = searchInfo;
    }

    @Override
    public void dismount(){
        super.dismount();
        if(loaders != null){
            for(Map.Entry<String, YouTubeThumbnailLoader> l : loaders.entrySet()){
                l.getValue().release();
            }
            loaders.clear();
        }
        if(initializing != null){
            initializing.clear();
        }
    }

    @Override
    public void init(){
        super.init();
        if(initialized && supportsYouTube){
            getVideos();
        }
    }

    @Override
    public void loadMoreBottom(final OnLoadListener listener){
        super.loadMoreBottom(listener);
        if(initialized && supportsYouTube) {
            getVideos();
        }
    }

    @Override
    public int getItemViewType(int position){
        if(getItems() == null || getItems().size() < 1){
            return EndlessScrollAdapter.DEFAULT_VIEW_TYPE;
        }else{
            JSONObject obj = getItem(position);
            if(obj.has("type")){
                try{
                    if(obj.getString("type").toLowerCase().equals("youtube")){
                        return YOUTUBE_VIEW_TYPE;
                    }
                }catch(JSONException j){
                    j.printStackTrace();
                }
            }
        }
        return -1;
    }

    @Override
    public EndlessScrollAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        super.onCreateViewHolder(parent, viewType); //super needs to be called here to set up the scroll listener
        if(viewType == YOUTUBE_VIEW_TYPE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.youtube_list_item, parent, false);
            YouTubeAdapter.ViewHolder vh = new YouTubeAdapter.ViewHolder(v);
            return vh;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(EndlessScrollAdapter.ViewHolder viewHolder, int position){
        try {
            final JSONObject item = getItem(position);
            if (viewHolder != null && getItemViewType(position) == YOUTUBE_VIEW_TYPE) {
                final YouTubeAdapter.ViewHolder vh = (YouTubeAdapter.ViewHolder) viewHolder;
                vh.thumbnailView.setImageBitmap(null);
                YouTubeThumbnailLoader l = getLoader(item);
                if (l == null) {
                    final YouTubeThumbnailView.OnInitializedListener loader = new YouTubeThumbnailView.OnInitializedListener() {
                        @Override
                        public void onInitializationSuccess(YouTubeThumbnailView youTubeThumbnailView, YouTubeThumbnailLoader youTubeThumbnailLoader) {
                            if (item.has("videoId") && getVideoId(item) != null) {
                                String videoId = getVideoId(item);
                                initializing.remove(videoId);
                                loaders.put(videoId, youTubeThumbnailLoader);
                                youTubeThumbnailLoader.setVideo(videoId);
                                vh.playView.setImageDrawable(new IconDrawable(context, Iconify.IconValue.fa_youtube_play)
                                        .colorRes(R.color.youtube_play_icon)
                                        .sizeDp(48));
                            }
                        }
                        @Override
                        public void onInitializationFailure(YouTubeThumbnailView youTubeThumbnailView, YouTubeInitializationResult youTubeInitializationResult) {
                            if(initializing.containsValue(youTubeThumbnailView)){
                                for(Map.Entry<String, YouTubeThumbnailView> e : initializing.entrySet()){
                                    if(e.getValue() != null && e.getValue().equals(youTubeThumbnailView)){
                                        initializing.remove(e.getKey());
                                    }
                                }
                            }
                        }
                    };
                    if(getVideoId(item) != null && !this.initializing.containsKey(getVideoId(item))) {
                        this.initializing.put(getVideoId(item), vh.thumbnailView);
                        vh.thumbnailView.initialize(getYouTubeId(), loader);
                    }else{
                        Runnable run = new Runnable() {
                            @Override
                            public void run() {
                                tryInitializingAgain(item, vh.thumbnailView, loader, 0);
                            }
                        };
                        Handler h = new Handler();
                        h.postDelayed(run, 1000);
                    }
                } else {
                    if (getVideoId(item) != null) {
                        l.setVideo(getVideoId(item));
                        vh.playView.setImageDrawable(new IconDrawable(context, Iconify.IconValue.fa_youtube_play)
                                .colorRes(R.color.youtube_play_icon)
                                .sizeDp(48));
                    }
                }
                vh.thumbnailView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String videoId = getVideoId(item);
                        context.startActivity(YouTubeStandalonePlayer.createVideoIntent((Activity) context,
                                getYouTubeId(), videoId, 0, true, true));
                    }
                });
                vh.youtubeTitle.setText(getVideoTitle(item));
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void tryInitializingAgain(final JSONObject item, final YouTubeThumbnailView thumbnailView,
                                        final YouTubeThumbnailView.OnInitializedListener loader, final int count){
        if(count < 3){
            if(getVideoId(item) != null && !this.initializing.containsKey(getVideoId(item))) {
                this.initializing.put(getVideoId(item), thumbnailView);
                thumbnailView.initialize(context.getString(R.string.google_api_developer_key), loader);
            }else{
                Runnable run = new Runnable() {
                    @Override
                    public void run() {
                        int c = count;
                        tryInitializingAgain(item, thumbnailView, loader, c++);
                    }
                };
                Handler h = new Handler();
                h.postDelayed(run, 1000);
            }
        }
    }

    public YouTubeThumbnailLoader getLoader(JSONObject item){
        try{
            if(item.has("videoId")){
                String videoId = item.getString("videoId");
                return loaders.get(videoId);
            }
        }catch(JSONException j){
            j.printStackTrace();
        }
        return null;
    }

    public String getVideoId(JSONObject item){
        try{
            if(item.has("videoId")){
                return item.getString("videoId");
            }
        }catch(JSONException j){
            j.printStackTrace();
        }
        return null;
    }

    public String getVideoTitle(JSONObject item){
        try{
            if(item.has("videoTitle")){
                return item.getString("videoTitle");
            }
        }catch(JSONException j){
            j.printStackTrace();
        }
        return null;
    }

    public void getVideos(){
        new AsyncTask<Void, Void, List<SearchResult>>() {
            @Override
            protected List<SearchResult> doInBackground(Void... params) {
                try {
                    String queryTerm = getSearchQuery();
                    // Define the API request for retrieving search results.
                    YouTube.Search.List search = youtube.search().list("id,snippet");
                    search.setKey(context.getString(R.string.google_api_developer_key));
                    search.setQ(queryTerm);
                    search.setType("video");
                    // To increase efficiency, only retrieve the fields that the
                    // application uses.
                    search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
                    search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                    //to make video selection more random, get a random date to set as published before
                    long offset = (new Date()).getTime(); //initialized to the current date and time
                    //need a range to choose for choosing the random date
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.YEAR, -2); //at most two years ago
                    long end = cal.getTimeInMillis(); //end of range
                    long diff = end - offset + 1;
                    search.setPublishedBefore(new com.google.api.client.util.DateTime(offset + (long)(Math.random() * diff)));
                    // Call the API
                    SearchListResponse searchResponse = search.execute();
                    List<SearchResult> searchResultList = searchResponse.getItems();
                    JSONObject obj;
                    for (SearchResult result : searchResultList) {
                        ResourceId rId = result.getId();
                        if (rId.getKind().equals("youtube#video")) {
                            obj = new JSONObject();
                            obj.put("videoId", rId.getVideoId());
                            obj.put("videoTitle", result.getSnippet().getTitle());
                            obj.put(getObjectIdField(), "video" + rId.getVideoId());
                            obj.put("type", "youtube");
                            int p = addAndPreventDuplicates(obj);
                            if(searchResultList.indexOf(result) == searchResultList.size() - 1){
                                lastAddedPosition = p;
                            }
                        }
                    }
                    return searchResultList;
                } catch (IllegalStateException e){
                    e.printStackTrace();
                } catch (GoogleJsonResponseException e) {
                    System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                            + e.getDetails().getMessage());
                } catch (IOException e) {
                    System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private int addAndPreventDuplicates(JSONObject obj){
        //slow but needed
        try {
            if(!obj.has("type") || !obj.getString("type").toLowerCase().equals("youtube") || !obj.has("videoId")){
                return -1;
            }
            boolean add = true;
            for(int i = 0; i < getItems().size(); i++){
                JSONObject o = getItem(i);
                if(o.has("type") && o.getString("type").toLowerCase().equals("youtube")){
                    if(o.has("videoId") && o.getString("videoId").equals(obj.getString("videoId"))){
                        add = false;
                        break;
                    }
                }
            }
            if(add){
                return addToRandomPosition(obj, lastAddedPosition);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    private String getSearchQuery(){
        try{
            if(searchInfo != null && searchInfo.length() > 0){
                if(searchInfo.length() == 1){
                    return searchInfo.getString(0);
                }else{
                    Random random = new Random();
                    int index = random.nextInt(searchInfo.length());
                    return searchInfo.getString(index);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public boolean doesSupportYouTube() {
        return supportsYouTube;
    }

    public void setSupportsYouTube(boolean supportsYouTube) {
        this.supportsYouTube = supportsYouTube;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public JSONArray getSearchInfo() {
        return searchInfo;
    }

    public String getYouTubeId(){
        return youTubeId;
    }

    public void setYouTubeId(String id){
        this.youTubeId = id;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public static class ViewHolder extends EndlessScrollAdapter.ViewHolder{
        com.google.android.youtube.player.YouTubeThumbnailView thumbnailView;
        TextView youtubeTitle;
        ImageView playView;

        public ViewHolder(View v){
            super(v);
            thumbnailView = (com.google.android.youtube.player.YouTubeThumbnailView) v.findViewById(R.id.youtube_thumbnail);
            youtubeTitle = (TextView) v.findViewById(R.id.youtube_title);
            playView = (ImageView) v.findViewById(R.id.youtube_play_button);
        }

    }

}
