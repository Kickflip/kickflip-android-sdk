package io.kickflip.sdk.view;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by davidbrodsky on 1/30/14.
 */
public class GLCameraView extends GLSurfaceView {
    private static final String TAG = "GLCameraView";

    protected ScaleGestureDetector mScaleGestureDetector;
    private Camera mCamera;
    private int mMaxZoom;

    public GLCameraView(Context context) {
        super(context);
        init(context);
    }

    public GLCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        mMaxZoom = 0;

    }

    public void setCamera(Camera camera){
        mCamera = camera;
        Camera.Parameters camParams = mCamera.getParameters();
        if(camParams.isZoomSupported()){
            mMaxZoom = camParams.getMaxZoom();
            mScaleGestureDetector = new ScaleGestureDetector(getContext(), mScaleListener);
        }
    }

    public void releaseCamera(){
        mCamera = null;
        mScaleGestureDetector = null;
    }

    private ScaleGestureDetector.SimpleOnScaleGestureListener mScaleListener = new ScaleGestureDetector.SimpleOnScaleGestureListener(){

        int mZoomWhenScaleBegan = 0;
        int mCurrentZoom = 0;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if(mCamera != null){
                Camera.Parameters params = mCamera.getParameters();
                mCurrentZoom = (int) (mZoomWhenScaleBegan + (mMaxZoom * (detector.getScaleFactor() - 1)));
                mCurrentZoom = Math.min(mCurrentZoom, mMaxZoom);
                mCurrentZoom = Math.max(0, mCurrentZoom);
                params.setZoom(mCurrentZoom);
                mCamera.setParameters(params);
            }

            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mZoomWhenScaleBegan =  mCamera.getParameters().getZoom();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(mScaleGestureDetector != null){
            if(!mScaleGestureDetector.onTouchEvent(ev)){
                // No scale gesture detected

            }
        }
        return true;
    }


}
