package io.kickflip.sdk;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import io.kickflip.sdk.fragment.BroadcastFragment;

public class BroadcastActivity extends Activity implements BroadcastListener, View.OnSystemUiVisibilityChangeListener{
    public static final String TAG = "BroadcastActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUi();
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

    private void hideSystemUi() {
        if (isKitKat()) {
            setImmersiveStickyMode(getWindow().getDecorView());
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        // Stay in low-profile mode
        if (isKitKat()){
            setImmersiveStickyMode(getWindow().getDecorView());
        }
    }

    private void setImmersiveStickyMode(View decorView) {
       decorView.setSystemUiVisibility(
               View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                       | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                       | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                       | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                       | View.SYSTEM_UI_FLAG_FULLSCREEN
                       | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
       );
    }

    private boolean isKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }
}
