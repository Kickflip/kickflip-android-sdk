package io.kickflip.sample;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import io.kickflip.sdk.av.RecorderConfig;
import io.kickflip.sdk.av.CameraRecorder;

/**
 * A simple example of using the CameraRecorder
 * to write streamable video to disk
 */
public class BroadcastFragment extends OAuthTestFragment {
    private static final String TAG = "BroadcastFragment";

    private static CameraRecorder mRecorder;        // Make static to survive Fragment re-creation
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
            RecorderConfig config = new RecorderConfig.Builder()
                    .withOuputFile(new File(Environment.getExternalStorageDirectory(), "kftest.mp4"))
                    .withVideoResolution(1280, 720)
                    .withVideoBitrate(2 * 1000 * 1000)
                    .build();

            mRecorder = new CameraRecorder(config);
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
        root.findViewById(R.id.recordButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder.isRecording())
                    mRecorder.stopRecording();
                else
                    mRecorder.startRecording();
            }
        });
        return root;
    }

}
