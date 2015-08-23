package com.chrynan.androidfeedscroll.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by chrynan on 8/22/2015.
 */
public class EndlessScrollCache {
    private Context context;
    private EndlessDbHelper mDbHelper;

    public EndlessScrollCache(Context context){
        this.context = context;
        mDbHelper = new EndlessDbHelper(context);
    }

    public void save(final JSONObject item){
        new CacheTask(){
            @Override
            protected List<JSONObject> doInBackground(Void... params) {
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                Iterator<String> it = item.keys();
                String s;
                ContentValues values = new ContentValues();
                try {
                    while ((s = it.next()) != null) {
                        EndlessScrollContract.EndlessEntry.columns.add(s);
                        values.put(s, item.getString(s));
                    }
                }catch(NoSuchElementException e){
                    //iterator might not check if is null and may just throw exception if there are no more items, so catch the exception
                }catch(JSONException je){
                    //may encounter an error parsing item value to String
                }
                db.insert(EndlessScrollContract.EndlessEntry.TABLE_NAME, null, values);
                List<JSONObject> items = new ArrayList<JSONObject>();
                items.add(item);
                db.close();
                return items;
            }
            @Override
            protected void onPostExecute(List<JSONObject> items) {}
        }.execute();
    }

    public void save(final List<JSONObject> items){
        new CacheTask(){
            @Override
            protected List<JSONObject> doInBackground(Void... params){
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                for(int i = 0; i < items.size(); i++) {
                    Iterator<String> it = items.get(i).keys();
                    String s;
                    ContentValues values = new ContentValues();
                    try {
                        while ((s = it.next()) != null) {
                            EndlessScrollContract.EndlessEntry.columns.add(s);
                            values.put(s, items.get(i).getString(s));
                        }
                        db.insert(EndlessScrollContract.EndlessEntry.TABLE_NAME, null, values);
                    } catch (NoSuchElementException e) {
                        //iterator might not check if is null and may just throw exception if there are no more items, so catch the exception
                    } catch (JSONException je) {
                        //may encounter an error parsing item value to String
                    }
                }
                db.close();
                return items;
            }
            @Override
            protected void onPostExecute(List<JSONObject> items){}
        }.execute();
    }

    public void cleanSave(final List<JSONObject> items){
        delete(new OnDeleteCacheListener() {
            @Override
            public void onDelete(String databaseName) {
                save(items);
            }
        });
    }

    public void delete(){
        new CacheTask(){
            @Override
            protected List<JSONObject> doInBackground(Void... params) {
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                if(db.isOpen()){
                    db.close();
                }
                context.deleteDatabase(mDbHelper.getDatabaseName());
                return null;
            }
            @Override
            protected void onPostExecute(List<JSONObject> item) {}
        };
    }

    public void delete(final OnDeleteCacheListener listener){
        new CacheTask(){
            @Override
            protected List<JSONObject> doInBackground(Void... params) {
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                if(db.isOpen()){
                    db.close();
                }
                context.deleteDatabase(mDbHelper.getDatabaseName());
                listener.onDelete(mDbHelper.getDatabaseName());
                return null;
            }
            @Override
            protected void onPostExecute(List<JSONObject> item) {}
        };
    }

    public void read(final OnReadCacheListener listener){
        new CacheTask(){
            @Override
            protected List<JSONObject> doInBackground(Void... params) {
                SQLiteDatabase db = mDbHelper.getReadableDatabase();
                Cursor c = db.query(EndlessScrollContract.EndlessEntry.TABLE_NAME, null, null, null, null, null, null);
                String[] columnNames = c.getColumnNames();
                int rowCount = c.getCount();
                c.moveToFirst();
                List<JSONObject> items = new ArrayList<>(rowCount);
                JSONObject obj;
                for(int i = 0; i < rowCount; i++){
                    try{
                        obj = new JSONObject();
                        for(int j = 0; j < columnNames.length; j++){
                            obj.put(columnNames[j], c.getString(c.getColumnIndexOrThrow(columnNames[j])));
                        }
                        items.add(obj);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                db.close();
                return items;
            }
            @Override
            protected void onPostExecute(List<JSONObject> items) {
                listener.onRead(items);
            }
        }.execute();
    }

    public interface OnReadCacheListener{
        void onRead(List<JSONObject> items);
    }

    public interface OnDeleteCacheListener{
        void onDelete(String databaseName);
    }

    public static abstract class CacheTask extends AsyncTask<Void, Void, List<JSONObject>>{
        public CacheTask(){};

        @Override
        protected abstract List<JSONObject> doInBackground(Void... params);
        @Override
        protected abstract void onPostExecute(List<JSONObject> item);
    }

}
