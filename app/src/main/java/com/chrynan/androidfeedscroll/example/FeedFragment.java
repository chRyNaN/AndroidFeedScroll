package com.chrynan.androidfeedscroll.example;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chrynan.androidfeedscroll.EndlessScrollAdapter;
import com.chrynan.androidfeedscroll.R;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by chrynan on 8/23/2015.
 */
public class FeedFragment extends Fragment {
    private String userId;
    private String token;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FeedAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    public static final Fragment newInstance(String userId, String token){
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putString("token", token);
        FeedFragment feed = new FeedFragment();
        feed.setArguments(args);
        return feed;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("userId");
            token = args.getString("token");
        }
        if (userId != null && token != null) {
            //simply create an instance of our adapter and everything else (loading, caching, scrolling, etc.) is handled internally
            mAdapter = new FeedAdapter(getActivity(), userId, token);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.feed_fragment, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.feed_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(mAdapter != null){
                    mAdapter.loadMoreTop(new EndlessScrollAdapter.OnLoadListener() {
                        @Override
                        public void onCacheLoad(List<JSONObject> items) {}
                        @Override
                        public void onLoad(List<JSONObject> items) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.peterRiver, R.color.turquoise);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.feed_recycler_view);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        if(mAdapter != null) {
            recyclerView.setAdapter(mAdapter);
        }
        return rootView;
    }

}
