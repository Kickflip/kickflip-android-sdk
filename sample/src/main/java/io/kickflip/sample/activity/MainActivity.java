package io.kickflip.sample.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

import io.kickflip.sample.MainFragmentInteractionListener;
import io.kickflip.sample.R;
import io.kickflip.sample.SECRETS;
import io.kickflip.sample.Util;
import io.kickflip.sample.fragment.MainFragment;
import io.kickflip.sample.fragment.StreamListFragment;
import io.kickflip.sdk.BroadcastListener;
import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.MediaPlayerActivity;
import io.kickflip.sdk.av.SessionConfig;
import io.kickflip.sdk.fragment.BroadcastFragment;


public class MainActivity extends Activity implements MainFragmentInteractionListener, StreamListFragment.StreamListFragmenListener {
    private static final String TAG = "MainActivity";
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
            // you'll want to remove/replace BroadcastFragment
            // when the Broadcast is over.

            //getFragmentManager().beginTransaction()
            //    .replace(R.id.container, MainFragment.getInstance())
            //    .commit();
        }

        @Override
        public void onBroadcastError() {
            Log.i(TAG, "onBroadcastError");
        }
    };
    // By default, Kickflip stores video in a "Kickflip" directory on external storage
    private String mRecordingOutputPath = new File(Environment.getExternalStorageDirectory(), "MySampleApp/index.m3u8").getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayShowHomeEnabled(false);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new StreamListFragment())
                    .commit();
        }

        Kickflip.setupWithApiKey(SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_broadcast:
                startBroadcastingActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentEvent(MainFragment.EVENT event) {
        startBroadcastingActivity();
    }

    /**
     * Unused method demonstrating how to use
     * Kickflip's BroadcastFragment.
     * <p/>
     * Note that in this scenario your Activity is responsible for
     * removing the BroadcastFragment in your onBroadcastStop callback.
     * When the user stops recording, the BroadcastFragment begins releasing
     * resources and freezes the camera preview.
     */
    public void startBroadcastFragment() {
        // Before using the BroadcastFragment, be sure to
        // register your BroadcastListener with Kickflip
        configureNewBroadcast();
        Kickflip.setBroadcastListener(mBroadcastListener);
        getFragmentManager().beginTransaction()
                .replace(R.id.container, BroadcastFragment.getInstance())
                .commit();
    }


    @Override
    public void onStreamPlaybackRequested(String streamUrl) {
        // Play with Kickflip's built-in Media Player
        Intent playbackIntent = new Intent(this, MediaPlayerActivity.class);
        playbackIntent.putExtra("mediaUrl", streamUrl);
        startActivity(playbackIntent);

        // Play with system Media Player
        //Intent i = new Intent(Intent.ACTION_VIEW);
        //i.setDataAndType(Uri.parse(stream.getStreamUrl()), "application/vnd.apple.mpegurl");
        //startActivity(i);
    }

    private void startBroadcastingActivity() {
        configureNewBroadcast();
        Kickflip.startBroadcastActivity(this, mBroadcastListener);
    }

    private void configureNewBroadcast() {
        SessionConfig config = new SessionConfig.Builder(mRecordingOutputPath)
                .withTitle(Util.getHumanDateString())
                .withDescription("Example Description")
                .withExtraInfo("{'foo': 'bar'}")
                .withPrivateVisibility(false)
                .withLocation(true)
                .withVideoResolution(1280, 720)
                .build();
        Kickflip.setSessionConfig(config);
    }
}
