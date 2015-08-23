package com.chrynan.androidfeedscroll.cache;

import android.provider.BaseColumns;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by chrynan on 8/22/2015.
 */
public final class EndlessScrollContract {

    public EndlessScrollContract(){};

    public static abstract class EndlessEntry implements BaseColumns{
        public static final String TABLE_NAME = "endless_entry";
        public static final Set<String> columns = new HashSet<>();
        private static final String TEXT_TYPE = " TEXT";
        private static final String COMMA_SEP = ",";

        public static final String getCreateQueriesString(){
            StringBuilder sb = new StringBuilder("CREATE TABLE " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY");
            for(String s : columns){
                sb.append(",");
                sb.append(s + TEXT_TYPE);
            }
            return sb.toString();
        }

        public static final String getDeleteQueriesString(){
            return "DROP TABLE IF EXISTS " + TABLE_NAME;
        }
    }

}
