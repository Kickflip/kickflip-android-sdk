package io.kickflip.sdk.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import io.kickflip.sdk.KickflipApiClient;
import io.kickflip.sdk.KickflipAuthCallback;
import io.kickflip.sdk.json.KickflipAwsResponse;


/**
 * This is a convenience Fragment that holds
 * a Kickflip Key / Secret pair
 */
public class KickflipFragment extends Fragment {
    private static final String TAG = "KickflipFragment";

    protected static final String ARG_CLIENT_KEY = "key";
    protected static final String ARG_CLIENT_SECRET = "secret";

    private String mClientKey;
    private String mClientSecret;

    public KickflipFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mClientKey = getArguments().getString(ARG_CLIENT_KEY);
            mClientSecret = getArguments().getString(ARG_CLIENT_SECRET);
        } else {
            Log.w(TAG, "No client credentials provided! This fragment won't do anything");
        }
    }

    public String getClientKey() {
        return mClientKey;
    }

    public String getClientSecret() {
        return mClientSecret;
    }

}
