package io.kickflip.sdk.av;

import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;


/**
 * Created by davidbrodsky on 1/22/14.
 */
public class EglStateSaver {

    private EGLContext mSavedContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mSavedReadSurface = EGL14.EGL_NO_SURFACE;
    private EGLSurface mSavedDrawSurface = EGL14.EGL_NO_SURFACE;
    private EGLDisplay mSavedDisplay = EGL14.EGL_NO_DISPLAY;

    public void saveEGLState(){
        mSavedContext = EGL14.eglGetCurrentContext();
        mSavedReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
        mSavedDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        mSavedDisplay = EGL14.eglGetCurrentDisplay();
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

}
