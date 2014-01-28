package io.kickflip.sample;

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

import io.kickflip.sdk.av.AVRecorder;
import io.kickflip.sdk.av.CameraEncoder;
import io.kickflip.sdk.av.RecorderConfig;

/**
 * A simple example of using the CameraEncoder
 * to write streamable video to disk
 */
public class BroadcastFragment extends OAuthTestFragment implements AdapterView.OnItemSelectedListener{
    private static final String TAG = "BroadcastFragment";

    private static AVRecorder mRecorder;        // Make static to survive Fragment re-creation
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
        if(mRecorder == null){

            File container = new File(Environment.getExternalStorageDirectory(), "Kickflip");
            container.mkdir();
            File output = new File(container, "kftest.m3u8");

            RecorderConfig config = new RecorderConfig.Builder(output)
                    .withVideoResolution(1280, 720)
                    .withVideoBitrate(2 * 1000 * 1000)
                    .withAudioBitrate(96 * 1000)
                    .build();

            mRecorder = new AVRecorder(config);
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
        mRecorder.setPreviewDisplay(mGLSurfaceView);
        Button recordButton = (Button) root.findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder.isRecording()) {
                    mRecorder.stopRecording();
                    ((Button) v).setText(R.string.record);
                } else {
                    mRecorder.startRecording();
                    ((Button) v).setText(R.string.stop);
                }
            }
        });

        setupFilterSpinner(root);
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int filterNum = ((Spinner) parent).getSelectedItemPosition();
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override public void run() {
                // notify the renderer that we want to change the encoder's state
                mRecorder.applyFilter(filterNum);
            }
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
