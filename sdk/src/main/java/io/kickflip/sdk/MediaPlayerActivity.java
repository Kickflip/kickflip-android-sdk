package io.kickflip.sdk;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import io.kickflip.sdk.fragment.MediaPlayerFragment;


public class MediaPlayerActivity extends ImmersiveActivity {
    private static final String TAG = "MediaPlayerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setUseImmersiveMode(false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_playback);
        String mediaUrl = getIntent().getStringExtra("mediaUrl");
        if (mediaUrl == null) {
            Log.i(TAG, "Activity created with no mediaUrl");
            finish();
        }
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, MediaPlayerFragment.newInstance(mediaUrl))
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.media_playback, menu);
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

}
