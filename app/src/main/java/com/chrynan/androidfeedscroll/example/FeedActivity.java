package com.chrynan.androidfeedscroll.example;

import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;

import com.chrynan.androidfeedscroll.R;

/**
 * Created by chrynan on 8/23/2015.
 */
public class FeedActivity extends AppCompatActivity {
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_activity);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //you should get your applications credentials such as userId and token so you can pass it in to the adapter for REST calls
        //we'll use dummy values here
        Fragment feed = FeedFragment.newInstance("123456", "dkeihtygri34j45");
        //add the fragment to the activity
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, feed).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //make sure the actionbar doesn't display the up/back button
        ActionBar actionBar = getActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        //change color of the status bar if we have that support
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.midnightBlue));
        }
        return true;
    }

}
