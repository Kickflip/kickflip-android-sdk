package io.kickflip.sdk.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import io.kickflip.sdk.av.CameraEncoder;
import io.kickflip.sdk.view.GLCameraView;

/**
 * Special GLSurfaceView for use with CameraEncoder
 * The tight coupling here allows richer touch interaction
 */
public class GLCameraEncoderView extends GLCameraView {
    private static final String TAG = "GLCameraEncoderView";

    protected CameraEncoder mCameraEncoder;

    public GLCameraEncoderView(Context context) {
        super(context);
    }

    public GLCameraEncoderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCameraEncoder(CameraEncoder encoder){
        mCameraEncoder = encoder;
        setCamera(mCameraEncoder.getCamera());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(mScaleGestureDetector != null){
            mScaleGestureDetector.onTouchEvent(ev);
        }
        if(mCameraEncoder != null && ev.getPointerCount() == 1 && (ev.getAction() == MotionEvent.ACTION_MOVE)){
            mCameraEncoder.handleCameraPreviewTouchEvent(ev);
        }else if(mCameraEncoder != null && ev.getPointerCount() == 1 && (ev.getAction() == MotionEvent.ACTION_DOWN)){
            mCameraEncoder.handleCameraPreviewTouchEvent(ev);
        }
        return true;
    }


}
