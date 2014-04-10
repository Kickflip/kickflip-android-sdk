package io.kickflip.sdk.av;

import io.kickflip.sdk.view.GLCameraView;

/**
 * Created by davidbrodsky on 1/24/14.
 */
public class AVRecorder {

    protected CameraEncoder mCamEncoder;
    protected MicrophoneEncoder mMicEncoder;
    private SessionConfig mConfig;
    private boolean mIsRecording;

    public AVRecorder(SessionConfig config){
        init(config);
    }

    private void init(SessionConfig config){
        mCamEncoder = new CameraEncoder(config);
        mMicEncoder = new MicrophoneEncoder(config);
        mConfig = config;
        mIsRecording = false;
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

    public void adjustVideoBitrate(int targetBitRate){
        mCamEncoder.adjustBitrate(targetBitRate);
    }

    public void startRecording(){
        mIsRecording = true;
        mMicEncoder.startRecording();
        mCamEncoder.startRecording();
    }

    public boolean isRecording(){
        return mIsRecording;
    }

    public void stopRecording(){
        mIsRecording = false;
        mCamEncoder.stopRecording();
        mMicEncoder.stopRecording();
    }

    public void reset(SessionConfig config){
        init(mConfig);
    }

    public void onHostActivityPaused(){
        mCamEncoder.onHostActivityPaused();
    }

    public void onHostActivityResumed(){
        mCamEncoder.onHostActivityResumed();
    }
}
