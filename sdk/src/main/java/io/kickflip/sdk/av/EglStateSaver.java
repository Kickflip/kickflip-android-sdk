package io.kickflip.sdk.av;

import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.util.Log;


/**
 * @hide
 */
public class EglStateSaver {
    private static final String TAG = "EglStateSaver";
    private static final boolean DEBUG = true;

    private EGLContext mSavedContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mSavedReadSurface = EGL14.EGL_NO_SURFACE;
    private EGLSurface mSavedDrawSurface = EGL14.EGL_NO_SURFACE;
    private EGLDisplay mSavedDisplay = EGL14.EGL_NO_DISPLAY;

    public void saveEGLState(){
        mSavedContext = EGL14.eglGetCurrentContext();
        if(DEBUG && mSavedContext.equals(EGL14.EGL_NO_CONTEXT)) Log.e(TAG, "Saved EGL_NO_CONTEXT");
        mSavedReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
        if(DEBUG && mSavedReadSurface.equals(EGL14.EGL_NO_CONTEXT)) Log.e(TAG, "Saved EGL_NO_SURFACE");
        mSavedDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        if(DEBUG && mSavedDrawSurface.equals(EGL14.EGL_NO_CONTEXT)) Log.e(TAG, "Saved EGL_NO_SURFACE");
        mSavedDisplay = EGL14.eglGetCurrentDisplay();
        if(DEBUG && mSavedDisplay.equals(EGL14.EGL_NO_DISPLAY)) Log.e(TAG, "Saved EGL_NO_DISPLAY");

    }

    public EGLContext getSavedEGLContext(){
        return mSavedContext;
    }

    public void makeSavedStateCurrent(){
        EGL14.eglMakeCurrent(mSavedDisplay, mSavedReadSurface, mSavedDrawSurface, mSavedContext);
    }

    public void makeNothingCurrent(){
        EGL14.eglMakeCurrent(mSavedDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
    }

    public void logState(){
        if(!mSavedContext.equals(EGL14.eglGetCurrentContext()))
            Log.i(TAG, "Saved context DOES NOT equal current.");
        else
            Log.i(TAG, "Saved context DOES equal current.");

        if(!mSavedReadSurface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_READ))){
            if(mSavedReadSurface.equals(EGL14.EGL_NO_SURFACE))
                Log.i(TAG, "Saved read surface is EGL_NO_SURFACE");
            else
                Log.i(TAG, "Saved read surface DOES NOT equal current.");
        }else
            Log.i(TAG, "Saved read surface DOES equal current.");

        if(!mSavedDrawSurface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW))){
            if(mSavedDrawSurface.equals(EGL14.EGL_NO_SURFACE))
                Log.i(TAG, "Saved draw surface is EGL_NO_SURFACE");
            else
                Log.i(TAG, "Saved draw surface DOES NOT equal current.");
        }else
            Log.i(TAG, "Saved draw surface DOES equal current.");

        if(!mSavedDisplay.equals(EGL14.eglGetCurrentDisplay()))
            Log.i(TAG, "Saved display DOES NOT equal current.");
        else
            Log.i(TAG, "Saved display DOES equal current.");
    }

}
