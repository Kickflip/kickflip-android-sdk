package io.kickflip.sdk.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import io.kickflip.sdk.BroadcastListener;
import io.kickflip.sdk.GLCameraEncoderView;
import io.kickflip.sdk.av.Broadcaster;
import io.kickflip.sdk.av.RecorderConfig;
import io.kickflip.sdk.R;

/**
 * This is the base
 */
public class BroadcastFragment extends KickflipFragment implements AdapterView.OnItemSelectedListener{
    private static final String TAG = "BroadcastFragment";

    protected static final String ARG_OUTPUT_PATH = "output_path";

    private BroadcastListener mListener;
    private static Broadcaster mBroadcaster;        // Make static to survive Fragment re-creation
    private static String mOutputPath;
    private GLCameraEncoderView mCameraView;


    public BroadcastFragment() {
        // Required empty public constructor
    }

    public static BroadcastFragment newInstance(String clientKey, String clientSecret, String outputPath) {
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
        super.onCreate(savedInstanceState);
        if(getArguments() != null && getArguments().containsKey(ARG_OUTPUT_PATH)){
            mOutputPath = getArguments().getString(ARG_OUTPUT_PATH);
            Log.i(TAG, "set outputPath " + mOutputPath);
        }else
            Log.w(TAG, "No output path specified! This fragment won't do anything!. " +
                    "Did you call BroadcastFragment#newinstance(KEY, SECRET, outputPath)?");

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        try {
            mListener = (BroadcastListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainFragmentInteractionListener");
        }

        // By making the recorder static we can allow
        // recording to continue beyond this fragment's
        // lifecycle! That means the user can minimize the app
        // or even turn off the screen without interrupting the recording!
        // If you don't want this behavior, call stopRecording
        // on your Fragment/Activity's onStop()
        if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            if(mBroadcaster == null){
                Log.i(TAG, "Setting up Broadcaster for output " + mOutputPath);
                Context context = getActivity().getApplicationContext();
                RecorderConfig config = new RecorderConfig.Builder(mOutputPath)
                        .withVideoResolution(1280, 720)
                        .withVideoBitrate(2 * 1000 * 1000)
                        .withAudioBitrate(96 * 1000)
                        .build();

                mBroadcaster = new Broadcaster(context, config, getClientKey(), getClientSecret());
            }
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
        View root;
        if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            root = inflater.inflate(R.layout.fragment_broadcast, container, false);
            mCameraView = (GLCameraEncoderView) root.findViewById(R.id.cameraPreview);
            mBroadcaster.setPreviewDisplay(mCameraView);
            Button recordButton = (Button) root.findViewById(R.id.recordButton);
            recordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBroadcaster.isRecording()) {
                        mBroadcaster.stopRecording();
                        ((Button) v).setText(R.string.record);
                        if(mListener != null)
                            mListener.onBroadcastStop();
                    } else {
                        mBroadcaster.startRecording();
                        ((Button) v).setText(R.string.stop);
                    }
                }
            });

            setupFilterSpinner(root);
            setupCameraFlipper(root);
        }else
            root = new View(container.getContext());
        return root;
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
}
