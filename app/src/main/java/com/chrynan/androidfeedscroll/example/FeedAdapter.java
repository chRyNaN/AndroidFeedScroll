package com.chrynan.androidfeedscroll.example;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chrynan.androidfeedscroll.EndlessScrollAdapter;
import com.chrynan.androidfeedscroll.R;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


/**
 * Created by chrynan on 8/23/2015.
 */
public class FeedAdapter extends EndlessScrollAdapter {
    private Context context;
    private String userId;
    private String token;

    public FeedAdapter(Context context, String userId, String token){
        super(context, userId, token);
        this.context = context;
        this.userId = userId;
        this.token = token;
        //initiate the adapter
        setLoadTopRestURL("https://example.com");
        setLoadBottomRestURL("https://example.com");
        setObjectIdField("id");
        init();
    }

    public FeedAdapter(Context context, List<JSONObject> posts, String userId, String token){
        super(context, userId, token);
        this.context = context;
        this.userId = userId;
        this.token = token;
        //initiate the adapter
        setLoadTopRestURL("");
        setLoadBottomRestURL("");
        setObjectIdField("");
        init();
    }

    @Override
    public FeedAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        super.onCreateViewHolder(parent, viewType);
        CardView card = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.feed_list_item, parent, false);
        ViewHolder vh = new ViewHolder(card);
        return vh;
    }

    @Override
    public void onBindViewHolder(EndlessScrollAdapter.ViewHolder holder, int position) {
        //TODO should better use generics in EndlessScrollAdapter to avoid casting
        FeedAdapter.ViewHolder vh = (FeedAdapter.ViewHolder) holder;
        JSONObject obj = getItem(position);
        try{
            if(obj.has("imageURL")) {
                Picasso.with(context).load(obj.getString("imageURL")).into(vh.image);
                vh.name.setText(obj.getString("imageURL"));
            }
        }catch(JSONException j){
            j.printStackTrace();
        }
    }


    public static class ViewHolder extends EndlessScrollAdapter.ViewHolder {
        ImageView image;
        TextView name;

        public ViewHolder(CardView v){
            super(v);
            image = (ImageView) v.findViewById(R.id.image);
            name = (TextView) v.findViewById(R.id.name);
        }

    }//end of ViewHolder

}//end of FeedAdapter
