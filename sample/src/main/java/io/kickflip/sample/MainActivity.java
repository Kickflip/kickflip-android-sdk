package io.kickflip.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import io.kickflip.sdk.BroadcastListener;
import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.fragment.BroadcastFragment;


public class MainActivity extends Activity implements  MainFragmentInteractionListener {
    private static final String TAG = "MainActivity";

    // By default, Kickflip stores video in a "Kickflip" directory on external storage
    private String mRecordingOutputPath = new File(Environment.getExternalStorageDirectory(), "MySampleApp").getAbsolutePath();

    private BroadcastListener mBroadcastListener = new BroadcastListener() {
        @Override
        public void onBroadcastStart() {
            Log.i(TAG, "onBroadcastStart");
        }

        @Override
        public void onBroadcastLive(String watchUrl) {
            Log.i(TAG, "onBroadcastLive @ " + watchUrl);
        }

        @Override
        public void onBroadcastStop() {
            Log.i(TAG, "onBroadcastStop");

            // If you're manually injecting the BroadcastFragment,
            // here is where you'll want to remove/replace BroadcastFragment

            //getFragmentManager().beginTransaction()
            //    .replace(R.id.container, MainFragment.newInstance())
            //    .commit();
        }

        @Override
        public void onBroadcastError() {
            Log.i(TAG, "onBroadcastError");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commit();
        }

        Kickflip.setupWithApiKey(SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET);
        Kickflip.setOutputDirectory(mRecordingOutputPath);
    }

    @Override
    public void onFragmentEvent(MainFragment.EVENT event) {
        Kickflip.startBroadcastActivity(this, mBroadcastListener);
    }

    /**
     * Unused method demonstrating how to use
     * Kickflip's BroadcastFragment.
     *
     * Note that in this scenario your Activity is responsible for
     * removing the BroadcastFragment in your onBroadcastStop callback.
     * When the user stops recording, the BroadcastFragment begins releasing
     * resources and freezes the camera preview.
     *
     */
    public void startBroadcastFragment(){
        // Before using the BroadcastFragment, be sure to
        // register your BroadcastListener with Kickflip
        Kickflip.setBroadcastListener(mBroadcastListener);
        getFragmentManager().beginTransaction()
                .replace(R.id.container, BroadcastFragment.newInstance(mRecordingOutputPath))
                .commit();
    }
}
