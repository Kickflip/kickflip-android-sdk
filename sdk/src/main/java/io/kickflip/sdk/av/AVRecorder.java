package io.kickflip.sdk.av;

import io.kickflip.sdk.GLCameraView;

/**
 * Created by davidbrodsky on 1/24/14.
 */
public class AVRecorder {

    protected CameraEncoder mCamEncoder;
    protected MicrophoneEncoder mMicEncoder;
    private RecorderConfig mConfig;
    private boolean mIsRecording;

    public AVRecorder(RecorderConfig config){
        init(config);
    }

    private void init(RecorderConfig config){
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

    public void adjustBitrate(int targetBitRate){
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
