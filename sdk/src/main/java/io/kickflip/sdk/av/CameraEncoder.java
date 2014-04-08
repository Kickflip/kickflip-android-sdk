package io.kickflip.sdk.av;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import io.kickflip.sdk.view.GLCameraEncoderView;
import io.kickflip.sdk.view.GLCameraView;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by davidbrodsky on 1/22/14.
 */
public class CameraEncoder implements SurfaceTexture.OnFrameAvailableListener, Runnable {
    private static final String TAG = "CameraEncoder";
    private static final boolean TRACE = false;         // Systrace
    private static final boolean VERBOSE = false;       // Lots of logging

    // EncoderHandler Message types (Message#what)
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_SURFACE_TEXTURE = 3;
    private static final int MSG_REOPEN_CAMERA = 4;
    private static final int MSG_RELEASE_CAMERA = 5;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private FullFrameRect mFullScreen;
    private int mTextureId;
    private int mFrameNum;
    private VideoEncoderCore mVideoEncoder;
    private Camera mCamera;
    private SessionConfig mSessionConfig;
    private float[] mTransform = new float[16];
    private int mCurrentFilter;
    private int mNewFilter;
    private boolean mIncomingSizeUpdated;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;
    private EglStateSaver mEglSaver;

    private final Object mSurfaceTextureFence = new Object();   // guards mSurfaceTexture shared with GLSurfaceView.Renderer
    private SurfaceTexture mSurfaceTexture;
    private final Object mReadyForFrameFence = new Object();    // guards mReadyForFrames/mRecording
    private boolean mReadyForFrames;                            // Is the SurfaceTexture et all created
    private boolean mRecording;                                 // Are frames being recorded
    private boolean mEosRequested;                              // Should an EOS be sent on next frame. Used to stop encoder
    private final Object mReadyFence = new Object();            // guards ready/running
    private boolean mReady;                                     // mHandler created on Encoder thread
    private boolean mRunning;                                   // Encoder thread running

    private boolean mEncodedFirstFrame;
    private long mStartTimeNs;

    private GLSurfaceView mDisplayView;
    private CameraSurfaceRenderer mDisplayRenderer;

    private int mCurrentCamera;
    private int mDesiredCamera;

    private boolean mThumbnailRequested;
    private int mThumbnailScaleFactor;
    private int mThumbnailRequestedOnFrame;

    public CameraEncoder(SessionConfig config) {
        mEncodedFirstFrame = false;
        mReadyForFrames = false;
        mRecording = false;
        mEosRequested = false;

        mCurrentCamera = -1;
        mDesiredCamera = Camera.CameraInfo.CAMERA_FACING_BACK;

        mCurrentFilter = -1;
        mNewFilter = Filters.FILTER_NONE;

        mThumbnailRequested = false;
        mThumbnailRequestedOnFrame = -1;

        mSessionConfig = checkNotNull(config);
        mEglSaver = new EglStateSaver();
        startEncodingThread();
    }

    public SessionConfig getConfig() {
        return mSessionConfig;
    }

    /**
     * Request the device camera not currently selected
     * be made active. This will take effect immediately
     * or as soon as the camera preview becomes active.
     * <p/>
     * Called from UI thread
     */
    public void requestOtherCamera() {
        int otherCamera = 0;
        if (mCurrentCamera == 0)
            otherCamera = 1;
        requestCamera(otherCamera);
    }

    /**
     * Request a Camera by cameraId. This will take effect immediately
     * or as soon as the camera preview becomes active.
     * <p/>
     * Called from UI thread
     *
     * @param camera
     */
    public void requestCamera(int camera) {
        if (Camera.getNumberOfCameras() == 1) {
            Log.w(TAG, "Ignoring requestCamera: only one device camera available.");
            return;
        }
        mDesiredCamera = camera;
        if (mCamera != null && mDesiredCamera != mCurrentCamera) {
            // Hot swap camera
            mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE_CAMERA));
            mHandler.sendMessage(mHandler.obtainMessage(MSG_REOPEN_CAMERA));
        }
    }

    /**
     * Request a thumbnail be generated from
     * the next available frame
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     *                    produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnail(int scaleFactor) {
        mThumbnailRequested = true;
        mThumbnailScaleFactor = scaleFactor;
        mThumbnailRequestedOnFrame = -1;
    }

    /**
     * Request a thumbnail be generated from
     * the given frame
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     *                    produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnailOnFrameWithScaling(int frame, int scaleFactor) {
        mThumbnailScaleFactor = scaleFactor;
        mThumbnailRequestedOnFrame = frame;
    }

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size.
     * <p/>
     * TODO: should do a best-fit match.
     */
    private static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null && VERBOSE) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }

    public void adjustBitrate(int targetBitrate) {
        mVideoEncoder.adjustBitrate(targetBitrate);
    }

    public void logSavedEglState() {
        mEglSaver.logState();
    }

    public void setPreviewDisplay(GLCameraView display) {
        checkNotNull(display);
        mDisplayRenderer = new CameraSurfaceRenderer(display, this);
        // Prep GLSurfaceView and attach Renderer
        display.setEGLContextClientVersion(2);
        display.setRenderer(mDisplayRenderer);
        //display.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        display.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        display.setPreserveEGLContextOnPause(true);
        mDisplayView = display;
    }

    /**
     * Called from GLSurfaceView.Renderer thread
     *
     * @return The SurfaceTexture containing the camera frame to display. The
     * display EGLContext is current on the calling thread
     * when this call completes
     */
    public SurfaceTexture getSurfaceTextureForDisplay() {
        synchronized (mSurfaceTextureFence) {
            if (mSurfaceTexture == null)
                Log.w(TAG, "getSurfaceTextureForDisplay called before ST created");
            else {
                mEglSaver.makeSavedStateCurrent();
            }
            return mSurfaceTexture;
        }
    }

    public boolean isSurfaceTextureReadyForDisplay() {
        synchronized (mSurfaceTextureFence) {
            return !(mSurfaceTexture == null);
        }
    }

    /**
     * Apply a filter to the camera input
     * TODO: Apply to display and encoder
     *
     * @param filter
     */
    public void applyFilter(int filter) {
        Filters.checkFilterArgument(filter);
        mDisplayRenderer.changeFilterMode(filter);
        synchronized (mReadyForFrameFence) {
            mNewFilter = filter;
        }
    }

    /**
     * Notify the preview and encoder programs
     * of a touch event. Used by
     * GLCameraEncoderView
     *
     * @param ev onTouchEvent event to handle
     */
    public void handleCameraPreviewTouchEvent(MotionEvent ev) {
        mFullScreen.handleTouchEvent(ev);
        mDisplayRenderer.handleTouchEvent(ev);
    }

    /**
     * Called from UI thread
     *
     * @return is the CameraEncoder in the recording state
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRecording;
        }
    }

    /**
     * Called from UI thread
     */
    public void startRecording() {
        synchronized (mReadyForFrameFence) {
            mFrameNum = 0;
            mRecording = true;
        }
    }

    private void startEncodingThread() {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread running when start requested");
                return;
            }
            mRunning = true;
            new Thread(this, "CameraEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    /**
     * Called from UI thread
     */
    public void stopRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
    }

    /**
     * Called on Encoder thread
     */
    private void handleStopRecording() {
        synchronized (mReadyForFrameFence) {
            mEosRequested = true;
            if (VERBOSE) Log.i(TAG, "handleStopRecording");
        }
    }

    /**
     * Called on next call to handleFrameAavilable
     * following handleStopRecording. Last frame is submitted
     * to encoder, and drainEncoder(true) has been called.
     * Safe to release resources
     * <p/>
     * Called on Encoder thread
     */
    private void shutdown() {
        releaseEncoder();
        releaseCamera();
        Looper.myLooper().quit();
    }

    /**
     * Called on an "arbitrary thread"
     *
     * @param surfaceTexture the SurfaceTexture initiating the call
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // Pass SurfaceTexture to Encoding thread via Handler
        // Then Encode and display frame
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, surfaceTexture));
    }


    /**
     * Called on Encoder thread
     *
     * @param surfaceTexture the SurfaceTexure that initiated the call to onFrameAvailable
     */
    private void handleFrameAvailable(SurfaceTexture surfaceTexture) {
        if (TRACE) Trace.beginSection("handleFrameAvail");
        synchronized (mReadyForFrameFence) {
            if (!mReadyForFrames) {
                if (VERBOSE) Log.i(TAG, "Ignoring available frame, not ready");
                return;
            }
            mFrameNum++;
            if (VERBOSE && (mFrameNum % 30 == 0)) Log.i(TAG, "handleFrameAvailable");
            if (!surfaceTexture.equals(mSurfaceTexture))
                Log.w(TAG, "SurfaceTexture from OnFrameAvailable does not match saved SurfaceTexture!");
            mInputWindowSurface.makeCurrent();
            mSurfaceTexture.updateTexImage();

            if (mRecording) {
                if (TRACE) Trace.beginSection("drainVEncoder");
                mVideoEncoder.drainEncoder(false);
                if (TRACE) Trace.endSection();
                if (mCurrentFilter != mNewFilter) {
                    Filters.updateFilter(mFullScreen, mNewFilter);
                    mCurrentFilter = mNewFilter;
                    mIncomingSizeUpdated = true;
                }

                if (mIncomingSizeUpdated) {
                    mFullScreen.getProgram().setTexSize(mSessionConfig.getVideoWidth(), mSessionConfig.getVideoHeight());
                    mIncomingSizeUpdated = false;
                }

                surfaceTexture.getTransformMatrix(mTransform);
                if (TRACE) Trace.beginSection("drawVEncoderFrame");
                mFullScreen.drawFrame(mTextureId, mTransform);
                if (TRACE) Trace.endSection();
                if (!mEncodedFirstFrame) {
                    mStartTimeNs = mSurfaceTexture.getTimestamp();
                    mEncodedFirstFrame = true;
                }

                if (mThumbnailRequestedOnFrame == mFrameNum) {
                    mThumbnailRequested = true;
                }
                if (mThumbnailRequested) {
                    saveFrameAsImage();
                    mThumbnailRequested = false;
                }

                mInputWindowSurface.setPresentationTime(mSurfaceTexture.getTimestamp() - mStartTimeNs);
                mInputWindowSurface.swapBuffers();

                if (mEosRequested) {
                    if (VERBOSE) Log.i(TAG, "Sent last video frame. Draining encoder");
                    mVideoEncoder.drainEncoder(true);
                    mRecording = false;
                    mEosRequested = false;
                    shutdown();
                }
            }
        }

        // Signal GLSurfaceView to render
        mDisplayView.requestRender();

        if (TRACE) Trace.endSection();
    }

    private void saveFrameAsImage() {
        try {
            File recordingDir = new File(mSessionConfig.getMuxer().getOutputPath()).getParentFile();
            File imageFile = new File(recordingDir, String.format("%d.jpg", System.currentTimeMillis()));
            mInputWindowSurface.saveFrame(imageFile, mThumbnailScaleFactor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hook for Host Activity's onPause()
     * Called on UI thread
     */
    public void onHostActivityPaused() {
        synchronized (mReadyForFrameFence) {
            // Pause the GLSurfaceView's Renderer thread
            if (mDisplayView != null)
                mDisplayView.onPause();
            // Release camera if we're not recording
            if (!mRecording && mSurfaceTexture != null) {
                if (VERBOSE) Log.i("CameraRelease", "Releasing camera");
                if (mDisplayView != null)
                    releaseDisplayView();
                mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE_CAMERA));
            }
        }
    }

    /**
     * Hook for Host Activity's onResume()
     * Called on UI thread
     */
    public void onHostActivityResumed() {
        synchronized (mReadyForFrameFence) {
            // Resume the GLSurfaceView's Renderer thread
            if (mDisplayView != null)
                mDisplayView.onResume();
            // Re-open camera if we're not recording and the SurfaceTexture has already been created
            if (!mRecording && mSurfaceTexture != null) {
                if (VERBOSE)
                    Log.i("CameraRelease", "Opening camera and attaching to SurfaceTexture");
                mHandler.sendMessage(mHandler.obtainMessage(MSG_REOPEN_CAMERA));
            } else {
                Log.w("CameraRelease", "Didn't try to open camera onHAResume. rec: " + mRecording + " mSurfaceTexture ready? " + (mSurfaceTexture == null ? " no" : " yes"));
            }
        }
    }

    /**
     * The GLSurfaceView.Renderer calls here after creating a
     * new texture in the display rendering context. Use the
     * created textureId to create a SurfaceTexture for
     * connection to the camera
     * <p/>
     * Called on the GlSurfaceView.Renderer thread
     *
     * @param textureId the id of the texture bound to the new display surface
     */
    public void onSurfaceCreated(int textureId) {
        if (VERBOSE) Log.i(TAG, "onSurfaceCreated. Saving EGL State");
        synchronized (mReadyFence) {
            // The Host Activity lifecycle may go through a OnDestroy ... OnCreate ... OnSurfaceCreated ... OnPause ... OnStop...
            // on it's way out, so our real sense of bearing should come from whether the EncoderThread is running
            if (mReady) {
                mEglSaver.saveEGLState();
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_SURFACE_TEXTURE, textureId));
            }
        }
    }

    /**
     * Called on Encoder thread
     */
    private void handleSetSurfaceTexture(int textureId) {
        synchronized (mSurfaceTextureFence) {
            if (mSurfaceTexture != null) {
                // We're hot-swapping the display EGLContext after
                // creating the initial SurfaceTexture for camera display
                mInputWindowSurface.makeCurrent();
                mSurfaceTexture.detachFromGLContext();
                // Release the EGLSurface and EGLContext.
                mInputWindowSurface.releaseEglSurface();
                mFullScreen.release();
                mEglCore.release();

                // Create a new EGLContext and recreate the window surface.
                mEglCore = new EglCore(mEglSaver.getSavedEGLContext(), EglCore.FLAG_RECORDABLE);
                mInputWindowSurface.recreate(mEglCore);
                mInputWindowSurface.makeCurrent();

                // Create new programs and such for the new context.
                mTextureId = textureId;
                mFullScreen = new FullFrameRect(
                        new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
                mFullScreen.getProgram().setTexSize(mSessionConfig.getVideoWidth(), mSessionConfig.getVideoHeight());
                mIncomingSizeUpdated = true;
                mSurfaceTexture.attachToGLContext(mTextureId);
                //mEglSaver.makeNothingCurrent();
            } else {
                // We're setting up the intial SurfaceTexure pre-recording
                prepareEncoder(mEglSaver.getSavedEGLContext(),
                        mSessionConfig.getVideoWidth(),
                        mSessionConfig.getVideoHeight(),
                        mSessionConfig.getVideoBitrate(),
                        mSessionConfig.getMuxer());
                mTextureId = textureId;
                mSurfaceTexture = new SurfaceTexture(mTextureId);
                if (VERBOSE)
                    Log.i(TAG + "-SurfaceTexture", " SurfaceTexture created. pre setOnFrameAvailableListener");
                mSurfaceTexture.setOnFrameAvailableListener(this);
                openAndAttachCameraToSurfaceTexture();
                mReadyForFrames = true;
            }
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        if (VERBOSE) Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    /**
     * Called with the display EGLContext current, on Encoder thread
     *
     * @param sharedContext The display EGLContext to be shared with the Encoder Surface's context.
     * @param width         the desired width of the encoder's video output
     * @param height        the desired height of the encoder's video output
     * @param bitRate       the desired bitrate of the video encoder
     * @param muxer         the desired output muxer
     */
    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate,
                                Muxer muxer) {
        mVideoEncoder = new VideoEncoderCore(width, height, bitRate, muxer);
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface());
        mInputWindowSurface.makeCurrent();

        //mFullScreen = new FullFrameRect(Texture2dProgram.ProgramType.TEXTURE_EXT);
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        mFullScreen.getProgram().setTexSize(width, height);
        mIncomingSizeUpdated = true;
    }

    private void releaseEncoder() {
        mVideoEncoder.release();
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release();
            mFullScreen = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }

        mSurfaceTexture = null;
    }

    private void openAndAttachCameraToSurfaceTexture() {
        openCamera(mSessionConfig.getVideoWidth(), mSessionConfig.getVideoHeight(), mDesiredCamera);
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();
            if (VERBOSE)
                Log.i("CameraRelease", "Opened / Started Camera preview. mDisplayView ready? " + (mDisplayView == null ? " no" : " yes"));
            if (mDisplayView != null) configureDisplayView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     */
    private void openCamera(int desiredWidth, int desiredHeight, int requestedCameraType) {
        // There's a confusing conflation of Camera index in Camera.open(i)
        // with Camera.getCameraInfo().facing values. However the API specifies that Camera.open(0)
        // will always be a rear-facing camera, and CAMERA_FACING_BACK = 0.
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        int targetCameraType = requestedCameraType;
        boolean triedAllCameras = false;
        cameraLoop:
        while (!triedAllCameras) {
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == targetCameraType) {
                    mCamera = Camera.open(i);
                    mCurrentCamera = targetCameraType;
                    break cameraLoop;
                }
            }
            if (mCamera == null) {
                if (targetCameraType == requestedCameraType)
                    targetCameraType = (requestedCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK);
                else
                    triedAllCameras = true;
            }

        }

        if (mCamera == null) {
            mCurrentCamera = -1;
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

        List<String> focusModes = parms.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            parms.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        List<int[]> fpsRanges = parms.getSupportedPreviewFpsRange();
        int[] maxFpsRange = fpsRanges.get(fpsRanges.size() - 1);
        parms.setPreviewFpsRange(maxFpsRange[0], maxFpsRange[1]);

        choosePreviewSize(parms, desiredWidth, desiredHeight);
        // leave the frame rate set to default
        mCamera.setParameters(parms);

        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);
        String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
        if (fpsRange[0] == fpsRange[1]) {
            previewFacts += " @" + (fpsRange[0] / 1000.0) + "fps";
        } else {
            previewFacts += " @" + (fpsRange[0] / 1000.0) + " - " + (fpsRange[1] / 1000.0) + "fps";
        }
        if (VERBOSE) Log.i(TAG, "Camera preview set: " + previewFacts);
    }

    public Camera getCamera() {
        return mCamera;
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mDisplayView != null)
            releaseDisplayView();
        if (mCamera != null) {
            if (VERBOSE) Log.d(TAG, "releasing camera");
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    /**
     * Communicate camera-ready state to our display view.
     * This method allows us to handle custom subclasses
     */
    private void configureDisplayView() {
        if (mDisplayView instanceof GLCameraEncoderView)
            ((GLCameraEncoderView) mDisplayView).setCameraEncoder(this);
        else if (mDisplayView instanceof GLCameraView)
            ((GLCameraView) mDisplayView).setCamera(mCamera);
    }

    /**
     * Communicate camera-released state to our display view.
     */
    private void releaseDisplayView() {
        if (mDisplayView instanceof GLCameraEncoderView) {
            ((GLCameraEncoderView) mDisplayView).releaseCamera();
        } else if (mDisplayView instanceof GLCameraView)
            ((GLCameraView) mDisplayView).releaseCamera();
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<CameraEncoder> mWeakEncoder;

        public EncoderHandler(CameraEncoder encoder) {
            mWeakEncoder = new WeakReference<CameraEncoder>(encoder);
        }

        /**
         * Called on Encoder thread
         *
         * @param inputMessage
         */
        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            CameraEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    encoder.handleSetSurfaceTexture((Integer) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    encoder.handleFrameAvailable((SurfaceTexture) obj);
                    break;
                case MSG_REOPEN_CAMERA:
                    encoder.openAndAttachCameraToSurfaceTexture();
                    break;
                case MSG_RELEASE_CAMERA:
                    encoder.releaseCamera();
                    break;
                default:
                    throw new RuntimeException("Unexpected msg what=" + what);
            }
        }
    }
}
