package io.kickflip.sample;

import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.io.File;

import io.kickflip.sdk.GLCameraEncoderView;
import io.kickflip.sdk.GLCameraView;
import io.kickflip.sdk.av.Broadcaster;
import io.kickflip.sdk.av.RecorderConfig;

/**
 * A simple example of using the CameraEncoder
 * to write streamable video to disk
 */
public class BroadcastFragment extends OAuthTestFragment implements AdapterView.OnItemSelectedListener{
    private static final String TAG = "BroadcastFragment";

    private static Broadcaster mBroadcaster;        // Make static to survive Fragment re-creation
    private GLCameraEncoderView mCameraView;


    public BroadcastFragment() {
        // Required empty public constructor
    }

    public static BroadcastFragment newInstance(String clientKey, String clientSecret) {
        BroadcastFragment fragment = new BroadcastFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_KEY, clientKey);
        args.putString(ARG_CLIENT_SECRET, clientSecret);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // By making the recorder static we can allow
        // recording to continue beyond this fragment's
        // lifecycle! That means the user can minimize the app
        // or even turn off the screen without interrupting the recording!
        // If you don't want this behavior, call stopRecording
        // on your Fragment/Activity's onStop()
        if(mBroadcaster == null){
            String path = "/sdcard/Kickflip/zen_test.mp4";
            //String path = "rtmp://live15.us-va.zencoder.io:1935/live/755f48db1a055734ce33a9da8ac5558e";

            Context context = getActivity().getApplicationContext();
            RecorderConfig config = new RecorderConfig.Builder(path)
                    .withVideoResolution(1280, 720)
                    .withVideoBitrate(2 * 1000 * 1000)
                    .withAudioBitrate(96 * 1000)
                    .build();

            mBroadcaster = new Broadcaster(context, config, SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET);
        }
        Log.i(TAG, String.format("Client Key (%s) Secret (%s)", getClientKey(), getClientSecret()));

    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        mBroadcaster.onHostActivityResumed();
    }


    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        mBroadcaster.onHostActivityPaused();
    }

    @Override
    public void onStop (){
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy (){
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_broadcast, container, false);
        mCameraView = (GLCameraEncoderView) root.findViewById(R.id.cameraPreview);
        mBroadcaster.setPreviewDisplay(mCameraView);
        Button recordButton = (Button) root.findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBroadcaster.isRecording()) {
                    mBroadcaster.stopRecording();
                    ((Button) v).setText(R.string.record);
                } else {
                    mBroadcaster.startRecording();
                    ((Button) v).setText(R.string.stop);
                }
            }
        });

        setupFilterSpinner(root);
        setupCameraFlipper(root);
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
            //final int filterNum = ((Spinner) parent).getSelectedItemPosition();
            mBroadcaster.applyFilter(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
