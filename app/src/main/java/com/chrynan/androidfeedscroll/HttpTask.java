package com.chrynan.androidfeedscroll;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

public abstract class HttpTask extends AsyncTask<String, Void, String> {

    public HttpTask(){
        super();
    }

    @Override
    protected String doInBackground(String... params){
        //params = url, type, parameters; ex: "https://chrynan.com/", "get", "userId=123456"
        String url = params[0];
        String type = null;
        String p = null;
        if(params.length > 1) {
            type = params[1];
        }
        if(params.length > 2) {
            p = params[2];
        }
        try{
            return downloadUrl(url, type, p);
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected abstract void onPostExecute(String result);

    public static boolean isConnectedToInternet(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if(info != null && info.isConnected()){
            return true;
        }else{
            return false;
        }
    }

    public static String decodeURIComponent(String s) {
        if (s == null) {
            return null;
        }

        String result = null;

        try {
            result = URLDecoder.decode(s, "UTF-8");
        }catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    public static String encodeURIComponent(String s) {
        String result = null;

        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        }catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String downloadUrl(String myurl, String type, String params) throws IOException {
        Log.d("App", "downloadUrl() method");
        InputStream is = null;
        try {
            URL url;
            if(params != null){
                url = new URL(myurl + params);
            }else {
                url = new URL(myurl);
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            if(type != null) {
                conn.setRequestMethod(type);
            }else{
                conn.setRequestMethod("GET");
            }
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int responseCode = conn.getResponseCode();
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is, responseCode);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int responseCode) throws IOException, UnsupportedEncodingException {
        Log.d("App", "readIt() method");
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            br = new BufferedReader(new InputStreamReader(stream));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        JSONObject obj = new JSONObject();
        try{
            obj.put("response", sb.toString());
            obj.put("responseCode", responseCode);
        }catch(JSONException jex){
            jex.printStackTrace();
        }
        Log.d("App", "readIt method: obj.toString() = " + obj.toString());
        return obj.toString();
    }

}
