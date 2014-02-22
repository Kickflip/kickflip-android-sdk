package io.kickflip.sdk;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by davidbrodsky on 2/21/14.
 */
public class Kickflip {

    private static String API_KEY;
    private static String API_SECRET;
    private static String ROOT_OUTPUT_DIR;          // Absolute path to root storage location

    private static BroadcastListener mBroadcastListener;

    public static void initWithApiKey(String key, String secret){
        API_KEY = key;
        API_SECRET = secret;
        ROOT_OUTPUT_DIR = new File(Environment.getExternalStorageDirectory(), "Kickflip").getAbsolutePath();
    }

    public static void setOutputDirectory(String outputDirectory){
        ROOT_OUTPUT_DIR = outputDirectory;
    }

    public static void startBroadcastActivity(Activity host, String outputPath, BroadcastListener listener){
        checkNotNull(listener);
        mBroadcastListener = listener;
        Intent broadcastIntent = new Intent(host, BroadcastActivity.class);
        broadcastIntent.putExtra(BroadcastActivity.ARG_KEY, API_KEY);
        broadcastIntent.putExtra(BroadcastActivity.ARG_SECRET, API_SECRET);
        broadcastIntent.putExtra(BroadcastActivity.ARG_OUTPUT_DIR, outputPath);
        host.startActivity(broadcastIntent);
    }

    public static void setBroadcastListener(BroadcastListener listener){
        mBroadcastListener = listener;
    }

    public static BroadcastListener getBroadcastListener(){
        return mBroadcastListener;
    }

    public static String getApiKey(){
        return API_KEY;
    }

    public static String getApiSecret(){
        return API_SECRET;
    }

    public static String getOutputDir(){
        return ROOT_OUTPUT_DIR;
    }

    public static boolean readyToBroadcast(){
        return API_KEY != null && API_SECRET != null;
    }



}
