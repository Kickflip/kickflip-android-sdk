package io.kickflip.sample;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
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
import java.util.UUID;

import io.kickflip.sdk.FileUtils;
import io.kickflip.sdk.av.AVRecorder;
import io.kickflip.sdk.av.Broadcaster;
import io.kickflip.sdk.av.RecorderConfig;

/**
 * A simple example of using the CameraEncoder
 * to write streamable video to disk
 */
public class BroadcastFragment extends OAuthTestFragment implements AdapterView.OnItemSelectedListener{
    private static final String TAG = "BroadcastFragment";

    private static Broadcaster mBroadcaster;        // Make static to survive Fragment re-creation
    private GLSurfaceView mGLSurfaceView;


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
        super.onCreate(savedInstanceState);

        // By making the recorder static we can allow
        // recording to continue beyond this fragment's
        // lifecycle! That means the user can minimize the app
        // or even turn off the screen without interrupting the recording!
        // If you don't want this behavior, call stopRecording
        // on your Fragment/Activity's onStop()
        if(mBroadcaster == null){

            File root = new File(Environment.getExternalStorageDirectory(), "Kickflip");
            root.mkdirs();

            RecorderConfig config = new RecorderConfig.Builder(root)
                    .withVideoResolution(1280, 720)
                    .withVideoBitrate(2 * 1000 * 1000)
                    .withAudioBitrate(96 * 1000)
                    .build();

            Context context = getActivity().getApplicationContext();
            mBroadcaster = new Broadcaster(context, config, SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET);
        }
        Log.i(TAG, String.format("Client Key (%s) Secret (%s)", getClientKey(), getClientSecret()));

    }

    @Override
    public void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_broadcast, container, false);
        mGLSurfaceView = (GLSurfaceView) root.findViewById(R.id.cameraPreview);
        mBroadcaster.setPreviewDisplay(mGLSurfaceView);
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
        setupCameraSpinner(root);
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

    private void setupCameraSpinner(View root){
        Spinner spinner = (Spinner) root.findViewById(R.id.cameraSpinner);
        if(Camera.getNumberOfCameras() == 1){
            spinner.setVisibility(View.GONE);
        }else{
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.camera_names, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner.
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(this);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(((String) parent.getTag()).compareTo("filter") == 0 ){
            //final int filterNum = ((Spinner) parent).getSelectedItemPosition();
            mBroadcaster.applyFilter(position);
        }else if(((String) parent.getTag()).compareTo("camera") == 0 ){
            mBroadcaster.requestCamera(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
