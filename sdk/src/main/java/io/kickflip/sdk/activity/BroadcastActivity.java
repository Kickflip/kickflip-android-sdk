package io.kickflip.sdk.activity;

import android.os.Bundle;

import io.kickflip.sdk.R;
import io.kickflip.sdk.fragment.BroadcastFragment;

/**
 * BroadcastActivity manages a single live broadcast. It's a thin wrapper around {@link io.kickflip.sdk.fragment.BroadcastFragment}
 */
public class BroadcastActivity extends ImmersiveActivity {
    private static final String TAG = "BroadcastActivity";

    private BroadcastFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);

        if (savedInstanceState == null) {
            mFragment = BroadcastFragment.getInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, mFragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (mFragment != null) {
            mFragment.stopBroadcasting();
        }
        super.onBackPressed();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

}
