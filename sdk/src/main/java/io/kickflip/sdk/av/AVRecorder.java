package io.kickflip.sdk.av;

import android.opengl.GLSurfaceView;
import android.util.Log;

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

    public void startRecording(){
        SyncEvent sync = new SyncEvent();
        mMicEncoder.startRecording(sync);
        synchronized (sync){
            try {
                sync.wait();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            Log.i("STREAMFLOW", "SyncEvent notified! Starting video encode!");
            mCamEncoder.startRecording(sync);
        }
    }

    public void stopRecording(){
        mCamEncoder.stopRecording();
        mMicEncoder.stopRecording();
    }

    public boolean isRecording(){
        return mCamEncoder.isRecording();
    }
}
