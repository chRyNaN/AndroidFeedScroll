package com.chrynan.androidfeedscroll.example;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.chrynan.androidfeedscroll.EndlessScrollAdapter;
import com.chrynan.androidfeedscroll.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by chrynan on 9/8/2015.
 */
public abstract class ExpandableAdapter<EVH extends ExpandableAdapter.ExpandableViewHolder> extends EndlessScrollAdapter {
    public static final int CHILD_VIEW_TYPE = 645;
    public static final int PARENT_VIEW_TYPE = 702;
    private boolean expandable;
    private boolean scrollable;
    //collection of child views that are currently displayed
    //the integer corresponds to the views position
    private Set<Integer> openChildViews;

    public ExpandableAdapter(Context context, String userId, String token){
        super(context, userId, token);
        expandable = true;
        scrollable = true;
        openChildViews = new HashSet<>();

    }

    public ExpandableAdapter(Context context, String userId, String token, boolean expandable){
        super(context, userId, token);
        this.expandable = expandable;
        scrollable = true;
        openChildViews = new HashSet<>();

    }

    @Override
    public ExpandableViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        //sub classes must override this method to provide their logic and actually instantiate a view holder
        //sub classes should make a call to super on this method so that it can handle whether or not to allow scrolling
        if(scrollable){
            super.onCreateViewHolder(parent, viewType);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(EndlessScrollAdapter.ViewHolder holder, final int position){
        //sub classes should override this method to provide their own binding logic
        //this instance of the method handles and binds the on click logic
        super.onBindViewHolder(holder, position);
        if(holder instanceof ExpandableViewHolder){
            final ExpandableViewHolder h = (ExpandableViewHolder) holder;
            if(h.parentView != null){
                h.parentView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(isExpandable()) {
                            toggleChildView(h, position);
                        }
                    }
                });
            }
        }
    }

    public void toggleChildView(ExpandableViewHolder h, int position){
        if(isDisplayingChildView(position)){
            hideChildView(h, position);
        }else{
            showChildView(h, position);
        }
    }

    public boolean isDisplayingChildView(int position){
        return openChildViews.contains(position);
    }

    public void showChildView(ExpandableViewHolder h, int position){
        if(!isDisplayingChildView(position) && isExpandable()){
            if(h.childView != null){
                h.childView.setVisibility(View.VISIBLE);
            }else{
                Log.e("ExpandableAdapter", "showChildView(): childView is null");
            }
            openChildViews.add(position);
        }
    }

    public void hideChildView(ExpandableViewHolder h, int position){
        if(isDisplayingChildView(position)){
            if(h.childView != null){
                h.childView.setVisibility(View.GONE);
            }else{
                Log.e("ExpandableAdapter", "hideChildView(): childView is null.");
            }
            openChildViews.remove(position);
        }
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }


    public static class ExpandableViewHolder extends EndlessScrollAdapter.ViewHolder{
        View container;
        View parentView;
        View childView;

        //avoid direct instantiation of this class; instead use newInstance method
        private ExpandableViewHolder(View container){
            super(container);
        }

        public static ExpandableViewHolder newInstance(View parentView, View childView){
            LinearLayout container = (LinearLayout) LayoutInflater.from(parentView.getContext()).inflate(R.layout.expandable_view_holder, (ViewGroup) parentView.getRootView(), false);
            parentView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                                        LinearLayout.LayoutParams.WRAP_CONTENT));
            childView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            container.addView(parentView);
            container.addView(childView);
            childView.setVisibility(View.GONE);
            ExpandableViewHolder vh = new ExpandableViewHolder(container);
            vh.parentView = parentView;
            vh.childView = childView;
            vh.container = container;
            return vh;
        }

    }

}
