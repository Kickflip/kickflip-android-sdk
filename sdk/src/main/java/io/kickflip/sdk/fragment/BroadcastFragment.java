package io.kickflip.sdk.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.eventbus.Subscribe;

import io.kickflip.sdk.BroadcastListener;
import io.kickflip.sdk.GLCameraEncoderView;
import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.R;
import io.kickflip.sdk.Share;
import io.kickflip.sdk.av.Broadcaster;
import io.kickflip.sdk.events.BroadcastIsBufferingEvent;
import io.kickflip.sdk.events.BroadcastIsLiveEvent;

/**
 * This is a drop-in video-streaming fragment.
 * Currently, only one BroadcastFragment may be instantiated at a time.
 */
public class BroadcastFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "BroadcastFragment";
    private static final boolean VERBOSE = false;

    private BroadcastListener mListener;
    private static BroadcastFragment mFragment;
    private static Broadcaster mBroadcaster;        // Make static to survive Fragment re-creation
    private GLCameraEncoderView mCameraView;
    private TextView mLiveBanner;


    public BroadcastFragment() {
        // Required empty public constructor
        if (VERBOSE) Log.i(TAG, "construct");
    }

    public static BroadcastFragment getInstance() {
        if(mFragment == null) {
            // We haven't yet created a BroadcastFragment instance
            mFragment = recreateBroadcastFragment();
        } else if(mBroadcaster != null && !mBroadcaster.isRecording()) {
            // We have a leftover BroadcastFragment but it is not recording
            // Treat it as finished, and recreate
            mFragment = recreateBroadcastFragment();
        }
        return mFragment;
    }

    private static BroadcastFragment recreateBroadcastFragment() {
        mBroadcaster = null;
        return new BroadcastFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (VERBOSE) Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (!Kickflip.readyToBroadcast()) {
            Log.e(TAG, "Kickflip not properly prepared by BroadcastFragment's onCreate. SessionConfig: " + Kickflip.getRecorderConfig() + " key " + Kickflip.getApiKey() + " secret " + Kickflip.getApiSecret());
        } else {
            setupBroadcaster();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (VERBOSE) Log.i(TAG, "onDestroy");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (VERBOSE) Log.i(TAG, "onAttach");
        try {
            mListener = (BroadcastListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBroadcaster != null)
            mBroadcaster.onHostActivityResumed();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mBroadcaster != null)
            mBroadcaster.onHostActivityPaused();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.i(TAG, "onCreateView");

        View root;
        if (mBroadcaster != null && getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            root = inflater.inflate(R.layout.fragment_broadcast, container, false);
            mCameraView = (GLCameraEncoderView) root.findViewById(R.id.cameraPreview);
            mLiveBanner = (TextView) root.findViewById(R.id.liveLabel);
            mBroadcaster.setPreviewDisplay(mCameraView);
            Button recordButton = (Button) root.findViewById(R.id.recordButton);

            recordButton.setOnClickListener(mRecordButtonClickListener);
            mLiveBanner.setOnClickListener(mShareButtonClickListener);

            if (mBroadcaster.isLive()) {
                setBannerToLiveState();
                mLiveBanner.setVisibility(View.VISIBLE);
            }
            if (mBroadcaster.isRecording())
                recordButton.setBackgroundResource(R.drawable.red_dot_stop);
            setupFilterSpinner(root);
            setupCameraFlipper(root);
        } else
            root = new View(container.getContext());
        return root;
    }

    private void setupBroadcaster() {
        // By making the recorder static we can allow
        // recording to continue beyond this fragment's
        // lifecycle! That means the user can minimize the app
        // or even turn off the screen without interrupting the recording!
        // If you don't want this behavior, call stopRecording
        // on your Fragment/Activity's onStop()
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mBroadcaster == null) {
                if (VERBOSE)
                    Log.i(TAG, "Setting up Broadcaster for output " + Kickflip.getRecorderConfig().getOutputPath() + " client key: " + Kickflip.getApiKey() + " secret: " + Kickflip.getApiSecret());
                // TODO: Don't start recording until stream start response, so we can determine stream type...
                //File outputFile = new File(new File(Kickflip.getOutputDirectory()), "index.m3u8");
                Context context = getActivity().getApplicationContext();
                mBroadcaster = new Broadcaster(context, Kickflip.getRecorderConfig(), Kickflip.getApiKey(), Kickflip.getApiSecret());
                mBroadcaster.getEventBus().register(this);
                mBroadcaster.setBroadcastListener(Kickflip.getBroadcastListener());
            }
        }
    }

    private void setupFilterSpinner(View root) {
        Spinner spinner = (Spinner) root.findViewById(R.id.filterSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.camera_filter_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    private void setupCameraFlipper(View root) {
        View flipper = root.findViewById(R.id.cameraFlipper);
        if (Camera.getNumberOfCameras() == 1) {
            flipper.setVisibility(View.GONE);
        } else {
            flipper.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBroadcaster.requestOtherCamera();
                }
            });
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (((String) parent.getTag()).compareTo("filter") == 0) {
            mBroadcaster.applyFilter(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Subscribe
    public void onBroadcastIsBuffering(BroadcastIsBufferingEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setBannerToBufferingState();
                showLiveBanner();
            }
        });
    }

    @Subscribe
    public void onBroadcastIsLive(final BroadcastIsLiveEvent liveEvent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    setBannerToLiveState(liveEvent.getWatchUrl());
                } catch (Exception e) {
                    Log.i(TAG, "onBroadcastIsLiveException");
                    e.printStackTrace();
                }
            }
        });
    }

    private void setBannerToBufferingState() {
        mLiveBanner.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        mLiveBanner.setBackgroundResource(R.drawable.buffering_bg);
        mLiveBanner.setTag(null);
        mLiveBanner.setText(getString(R.string.buffering));
    }

    private void setBannerToLiveState() {
        setBannerToLiveState(null);
    }

    private void setBannerToLiveState(String watchUrl) {
        mLiveBanner.setBackgroundResource(R.drawable.live_bg);
        Drawable img = getActivity().getResources().getDrawable(android.R.drawable.ic_menu_share);
        mLiveBanner.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        if (watchUrl != null)
            mLiveBanner.setTag(watchUrl);
        mLiveBanner.setText(getString(R.string.live));
    }

    private void showLiveBanner() {
        mLiveBanner.bringToFront();
        mLiveBanner.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_from_left));
        mLiveBanner.setVisibility(View.VISIBLE);
    }

    private void hideLiveBanner() {
        mLiveBanner.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_to_left));
        mLiveBanner.setVisibility(View.INVISIBLE);
    }

    /**
     * Force this fragment to stop broadcasting.
     * Useful if your application wants to stop broadcasting
     * when a user leaves the Activity hosting this fragment
     */
    public void stopBroadcasting() {
        mBroadcaster.stopRecording();
    }

    View.OnClickListener mRecordButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mBroadcaster.isRecording()) {
                mBroadcaster.stopRecording();
                hideLiveBanner();
                if (mListener != null)
                    mListener.onBroadcastStop();
            } else {
                mBroadcaster.startRecording();
                v.setBackgroundResource(R.drawable.red_dot_stop);
            }
        }
    };

    View.OnClickListener mShareButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getTag() != null) {
                Intent shareIntent = Share.createShareChooserIntentWithTitleAndUrl(getActivity(), getString(R.string.share_broadcast), (String) v.getTag());
                startActivity(shareIntent);
            }
        }
    };


}
