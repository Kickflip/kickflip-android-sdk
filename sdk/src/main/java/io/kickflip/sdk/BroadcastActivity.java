package io.kickflip.sdk;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import io.kickflip.sdk.fragment.BroadcastFragment;

public class BroadcastActivity extends Activity implements BroadcastListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_broadcast);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, BroadcastFragment.newInstance())
                    .commit();
        }
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
