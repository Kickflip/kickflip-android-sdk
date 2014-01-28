package io.kickflip.sdk.av;

import android.opengl.GLSurfaceView;

/**
 * Created by davidbrodsky on 1/24/14.
 */
public class AVRecorder {

    private CameraEncoder mCamEncoder;
    private MicrophoneEncoder mMicEncoder;

    private RecorderConfig mConfig;

    public AVRecorder(RecorderConfig config){
        mCamEncoder = new CameraEncoder(config);
        mMicEncoder = new MicrophoneEncoder(config);
        mConfig = config;
    }

    public void setPreviewDisplay(GLSurfaceView display){
        mCamEncoder.setPreviewDisplay(display);
    }

    public void applyFilter(int filter){
        mCamEncoder.applyFilter(filter);
    }

    public void startRecording(){
        mMicEncoder.startRecording();
        mCamEncoder.startRecording();
    }

    public void stopRecording(){
        mCamEncoder.stopRecording();
        mMicEncoder.stopRecording();
    }

    public boolean isRecording(){
        return mCamEncoder.isRecording();
    }
}
