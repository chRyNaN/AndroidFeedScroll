package com.chrynan.androidfeedscroll.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by chrynan on 8/22/2015.
 */
public class EndlessDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "endless.db";
    private static final int DATABASE_VERSION = 1;

    public EndlessDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public EndlessDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(EndlessScrollContract.EndlessEntry.getDeleteQueriesString());
        onCreate(db);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(EndlessScrollContract.EndlessEntry.getCreateQueriesString());
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public String getDatabaseName(){
        return DATABASE_NAME;
    }

    public int getDatabaseVersion(){
        return DATABASE_VERSION;
    }

}
