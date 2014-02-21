package io.kickflip.sdk.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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
import android.widget.Toast;

import com.google.common.eventbus.Subscribe;

import io.kickflip.sdk.BroadcastListener;
import io.kickflip.sdk.GLCameraEncoderView;
import io.kickflip.sdk.Share;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.av.Broadcaster;
import io.kickflip.sdk.av.RecorderConfig;
import io.kickflip.sdk.R;
import io.kickflip.sdk.events.BroadcastIsLiveEvent;

/**
 * This is a drop-in video-streaming fragment.
 * Currently, only one BroadcastFragment may be instantiated at a time.
 */
public class BroadcastFragment extends KickflipFragment implements AdapterView.OnItemSelectedListener{
    private static final String TAG = "BroadcastFragment";
    private static final boolean VERBOSE = false;

    protected static final String ARG_OUTPUT_PATH = "output_path";

    private BroadcastListener mListener;
    private static Broadcaster mBroadcaster;        // Make static to survive Fragment re-creation
    private static String mOutputPath;
    private GLCameraEncoderView mCameraView;
    private TextView mLiveBanner;


    public BroadcastFragment() {
        // Required empty public constructor
        if (VERBOSE) Log.i(TAG, "construct");
    }

    public static BroadcastFragment newInstance(String clientKey, String clientSecret, String outputPath) {
        if (VERBOSE) Log.i(TAG, "newInstance");
        // Ensure we're creating a new Broadcaster for each new Fragment
        mBroadcaster = null;
        BroadcastFragment fragment = new BroadcastFragment();
        Bundle args = new Bundle();
        // KickflipFragment args:
        args.putString(ARG_CLIENT_KEY, clientKey);
        args.putString(ARG_CLIENT_SECRET, clientSecret);
        // BroadcastFragment args:
        args.putString(ARG_OUTPUT_PATH, outputPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (VERBOSE) Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if(getArguments() != null && getArguments().containsKey(ARG_OUTPUT_PATH)){
            mOutputPath = getArguments().getString(ARG_OUTPUT_PATH);
            if (VERBOSE) Log.i(TAG, "set outputPath " + mOutputPath + " key " + getClientKey() + " secret " + getClientSecret());
        }else{
            Log.w(TAG, "No output path specified! This fragment won't do anything!. " +
                    "Did you call BroadcastFragment#newinstance(KEY, SECRET, outputPath)?");
        }
        setupBroadcaster();
    }

    @Override
    public void onDestroy(){
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
        if(mBroadcaster != null)
            mBroadcaster.onHostActivityResumed();
    }


    @Override
    public void onPause() {
        super.onPause();
        if(mBroadcaster != null)
            mBroadcaster.onHostActivityPaused();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.i(TAG, "onCreateView");

        View root;
        if(mBroadcaster != null && getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            root = inflater.inflate(R.layout.fragment_broadcast, container, false);
            mCameraView = (GLCameraEncoderView) root.findViewById(R.id.cameraPreview);
            mLiveBanner = (TextView) root.findViewById(R.id.liveLabel);
            mBroadcaster.setPreviewDisplay(mCameraView);
            Button recordButton = (Button) root.findViewById(R.id.recordButton);

            recordButton.setOnClickListener(mRecordButtonClickListener);
            mLiveBanner.setOnClickListener(mLiveBannerClickListener);

            if(mBroadcaster.isLive())
                mLiveBanner.setVisibility(View.VISIBLE);
            setupFilterSpinner(root);
            setupCameraFlipper(root);
        }else
            root = new View(container.getContext());
        return root;
    }

    private void setupBroadcaster(){
        // By making the recorder static we can allow
        // recording to continue beyond this fragment's
        // lifecycle! That means the user can minimize the app
        // or even turn off the screen without interrupting the recording!
        // If you don't want this behavior, call stopRecording
        // on your Fragment/Activity's onStop()
        if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            if(mBroadcaster == null){
                if (VERBOSE) Log.i(TAG, "Setting up Broadcaster for output " + mOutputPath + " client key: " + getClientKey() + " secret: " + getClientSecret());
                Context context = getActivity().getApplicationContext();
                RecorderConfig config = new RecorderConfig.Builder(mOutputPath)
                        .withVideoResolution(1280, 720)
                        .withVideoBitrate(2 * 1000 * 1000)
                        .withAudioBitrate(96 * 1000)
                        .build();

                mBroadcaster = new Broadcaster(context, config, getClientKey(), getClientSecret());
                mBroadcaster.getEventBus().register(this);
            }
        }
    }

    private void setupFilterSpinner(View root){
        Spinner spinner = (Spinner) root.findViewById(R.id.filterSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.camera_filter_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    private void setupCameraFlipper(View root){
        View flipper = root.findViewById(R.id.cameraFlipper);
        if(Camera.getNumberOfCameras() == 1){
            flipper.setVisibility(View.GONE);
        }else{
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
        if(((String) parent.getTag()).compareTo("filter") == 0 ){
            mBroadcaster.applyFilter(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Subscribe
    public void onBroadcastIsLive(final BroadcastIsLiveEvent liveEvent){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showLiveBanner(liveEvent.getWatchUrl());
            }
        });
    }

    private void showLiveBanner(String watchUrl){
        mLiveBanner.bringToFront();
        mLiveBanner.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_from_left));
        mLiveBanner.setTag(watchUrl);
        mLiveBanner.setVisibility(View.VISIBLE);
    }

    private void hideLiveBanner(){
        mLiveBanner.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_to_left));
        mLiveBanner.setVisibility(View.INVISIBLE);
        mLiveBanner.setTag(null);
    }

    View.OnClickListener mRecordButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mBroadcaster.isRecording()) {
                mBroadcaster.stopRecording();
                hideLiveBanner();
                if(mListener != null)
                    mListener.onBroadcastStop();
            } else {
                mBroadcaster.startRecording();
                v.setBackgroundResource(R.drawable.red_dot_stop);
            }
        }
    };

    View.OnClickListener mLiveBannerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mLiveBanner.getTag() != null){
                Intent shareIntent = Share.createShareChooserIntentWithTitleAndUrl(getActivity(), getString(R.string.share_broadcast), (String) mLiveBanner.getTag());
                startActivity(shareIntent);
            }
        }
    };


}
