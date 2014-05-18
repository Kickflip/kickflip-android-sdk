package io.kickflip.sdk.av;

import io.kickflip.sdk.view.GLCameraView;

/**
 * Records an Audio / Video stream to disk.
 *
 * Example usage:
 * <ul>
 *     <li>AVRecorder recorder = new AVRecorder(mSessionConfig);</li>
 *     <li>recorder.setPreviewDisplay(mPreviewDisplay);</li>
 *     <li>recorder.startRecording();</li>
 *     <li>recorder.stopRecording();</li>
 *     <li>(Optional) recorder.reset(mNewSessionConfig);</li>
 *     <li>(Optional) recorder.startRecording();</li>
 *     <li>(Optional) recorder.stopRecording();</li>
 *     <li>recorder.release();</li>
 * </ul>
 * @hide
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

        if(config.getRecordAudio())
            mMicEncoder = new MicrophoneEncoder(config);
        else
            mMicEncoder = null;

        mConfig = config;
        setupMuxer();
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

    /**
     * Signal that the recorder should treat
     * incoming video frames as Vertical Video, rotating
     * and cropping them for proper display.
     *
     * This method only has effect if {@link io.kickflip.sdk.av.SessionConfig#setConvertVerticalVideo(boolean)}
     * has been set true for the current recording session.
     *
     */
    public void signalVerticalVideo(FullFrameRect.SCREEN_ROTATION orientation) {
        mCamEncoder.signalVerticalVideo(orientation);
    }

    public void startRecording(){
        mIsRecording = true;
        if(mMicEncoder != null)
            mMicEncoder.startRecording();
        mCamEncoder.startRecording();
    }

    public boolean isRecording(){
        return mIsRecording;
    }

    public void stopRecording(){
        mIsRecording = false;
        if(mMicEncoder != null)
            mMicEncoder.stopRecording();
        mCamEncoder.stopRecording();
    }

    /**
     * Prepare for a subsequent recording. Must be called after {@link #stopRecording()}
     * and before {@link #release()}
     * @param config
     */
    public void reset(SessionConfig config){
        mCamEncoder.reset(config);

        if(config.getRecordAudio()) {
            if(mMicEncoder == null)
                mMicEncoder = new MicrophoneEncoder(config);
            else
                mMicEncoder.reset(config);
        }
        else {
            mMicEncoder = null;
        }

        mConfig = config;
        setupMuxer();
        mIsRecording = false;
    }

    private void setupMuxer() {
        if(mConfig.getRecordAudio()) {
            mConfig.getMuxer().setExpectedNumTracks(2);
        }
        else {
            mConfig.getMuxer().setExpectedNumTracks(1);
        }
    }

    /**
     * Release resources. Must be called after {@link #stopRecording()} After this call
     * this instance may no longer be used.
     */
    public void release() {
        mCamEncoder.release();
        // MicrophoneEncoder releases all it's resources when stopRecording is called
        // because it doesn't have any meaningful state
        // between recordings. It might someday if we decide to present
        // persistent audio volume meters etc.
        // Until then, we don't need to write MicrophoneEncoder.release()
    }

    public void onHostActivityPaused(){
        mCamEncoder.onHostActivityPaused();
    }

    public void onHostActivityResumed(){
        mCamEncoder.onHostActivityResumed();
    }
}
