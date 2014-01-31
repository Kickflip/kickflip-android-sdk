package io.kickflip.sdk.av;

import android.opengl.GLSurfaceView;

import io.kickflip.sdk.GLCameraView;

/**
 * Created by davidbrodsky on 1/24/14.
 */
public class AVRecorder {

    private CameraEncoder mCamEncoder;
    private MicrophoneEncoder mMicEncoder;

    private RecorderConfig mConfig;

    public AVRecorder(RecorderConfig config){
       init(config);
    }

    private void init(RecorderConfig config){
        mCamEncoder = new CameraEncoder(config);
        mMicEncoder = new MicrophoneEncoder(config);
        mConfig = config;
    }

    public void setPreviewDisplay(GLCameraView display){
        mCamEncoder.setPreviewDisplay(display);
    }

    public void applyFilter(int filter){
        mCamEncoder.applyFilter(filter);
    }

    public void requestOtherCamera(){
        mCamEncoder.requestOtherCamera();
    }

    public void requestCamera(int camera){
        mCamEncoder.requestCamera(camera);
    }

    public void startRecording(){
        mMicEncoder.startRecording();
        mCamEncoder.startRecording();
    }

    public boolean isRecording(){
        return mCamEncoder.isRecording();
    }

    public void stopRecording(){
        mCamEncoder.stopRecording();
        mMicEncoder.stopRecording();
    }

    public void reset(RecorderConfig config){
        init(mConfig);
    }

    public void onHostActivityPaused(){
        mCamEncoder.onHostActivityPaused();
    }

    public void onHostActivityResumed(){
        mCamEncoder.onHostActivityResumed();
    }
}
