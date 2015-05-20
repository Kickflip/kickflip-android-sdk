package io.kickflip.sdk.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import io.kickflip.sdk.R;
import io.kickflip.sdk.fragment.MediaPlayerFragment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @hide
 */
public class MediaPlayerActivity extends ImmersiveActivity {
    private static final String TAG = "MediaPlayerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_playback);
        // This must be setup before
        //Uri intentData = getIntent().getData();
        //String mediaUrl = isKickflipUrl(intentData) ? intentData.toString() : getIntent().getStringExtra("mediaUrl");
        String mediaUrl = getIntent().getStringExtra("mediaUrl");
        checkNotNull(mediaUrl, new IllegalStateException("MediaPlayerActivity started without a mediaUrl"));
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, MediaPlayerFragment.newInstance(mediaUrl))
                    .commit();
        }
    }
}
