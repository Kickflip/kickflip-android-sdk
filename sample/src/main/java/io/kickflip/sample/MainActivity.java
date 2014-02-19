package io.kickflip.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import io.kickflip.sdk.BroadcastListener;
import io.kickflip.sdk.fragment.BroadcastFragment;


public class MainActivity extends Activity implements BroadcastListener, MainFragmentInteractionListener {
    private String mRecordingOutputPath = "/sdcard/Kickflip/index.m3u8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // BroadcastListener methods

    @Override
    public void onBroadcastStart() {

    }

    @Override
    public void onBroadcastLive() {

    }

    @Override
    public void onBroadcastStop() {
        // Show main fragment
        getFragmentManager().beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .setCustomAnimations(R.anim.fade_out, R.anim.fade_in, R.anim.fade_out, R.anim.fade_in)
                .commit();
    }

    @Override
    public void onBroadcastError() {

    }

    @Override
    public void onFragmentEvent(MainFragment.EVENT event) {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, BroadcastFragment.newInstance(SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET, mRecordingOutputPath))
                .setCustomAnimations(R.anim.fade_out, R.anim.fade_in, R.anim.fade_out, R.anim.fade_in)
                .commit();
    }
}
