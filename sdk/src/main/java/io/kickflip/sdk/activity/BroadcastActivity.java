package io.kickflip.sdk.activity;

import android.os.Bundle;

import io.kickflip.sdk.av.BroadcastListener;
import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.R;
import io.kickflip.sdk.fragment.BroadcastFragment;

public class BroadcastActivity extends ImmersiveActivity implements BroadcastListener {
    public static final String TAG = "BroadcastActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, BroadcastFragment.getInstance())
                    .commit();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onBroadcastStart() {
        Kickflip.getBroadcastListener().onBroadcastStart();
    }

    @Override
    public void onBroadcastLive(String watchUrl) {
        Kickflip.getBroadcastListener().onBroadcastLive(watchUrl);
    }

    @Override
    public void onBroadcastStop() {
        Kickflip.getBroadcastListener().onBroadcastStop();
        this.finish();
    }

    @Override
    public void onBroadcastError() {
        Kickflip.getBroadcastListener().onBroadcastError();
    }

}
