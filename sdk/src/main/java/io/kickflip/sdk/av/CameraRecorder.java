package io.kickflip.sdk.av;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by davidbrodsky on 1/22/14.
 */
public class CameraRecorder implements SurfaceTexture.OnFrameAvailableListener, Runnable {
    private static final String TAG = "CameraRecorder";
    private static final boolean VERBOSE = false;

    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_SURFACE_TEXTURE = 3;
    //private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private FullFrameRect mFullScreen;
    private int mTextureId;
    private int mFrameNum;
    private VideoEncoderCore mVideoEncoder;

    private Camera mCamera;
    private RecorderConfig mRecorderConfig;
    private float[] mTransform = new float[16];

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;
    private EglStateSaver mEglSaver;

    private Object mSurfaceTextureFence = new Object(); // guards mSurfaceTexture shared with GLSurfaceView.Renderer
    private SurfaceTexture mSurfaceTexture;
    private Object mReadyForFrameFence = new Object();  // guards mReadyForFrames/mRecordingRequested
    private boolean mReadyForFrames;                    // Is the SurfaceTexture et all created
    private boolean mRecordingRequested;                // Is Recording desired
    private Object mReadyFence = new Object();          // guards ready/running
    private boolean mReady;                             // mHandler created on Encoder thread
    private boolean mRunning;                           // Encoder thread running

    private GLSurfaceView mDisplayView;

    public CameraRecorder(RecorderConfig config) {
        mReadyForFrames = false;
        mRecordingRequested = false;

        mRecorderConfig = config;
        mEglSaver = new EglStateSaver();
        startEncodingThread();
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
        if (ppsfv != null) {
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

    public void setPreviewDisplay(GLSurfaceView disp) {
        // Prep GLSurfaceView and attach Renderer
        disp.setEGLContextClientVersion(2);
        disp.setRenderer(new CameraSurfaceRenderer(this));
        disp.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        disp.setPreserveEGLContextOnPause(true);
        mDisplayView = disp;
    }

    /**
     * Called from GLSurfaceView.Renderer thread
     *
     * @return
     */
    public SurfaceTexture getSurfaceTextureForDisplay() {
        synchronized (mSurfaceTextureFence) {
            mEglSaver.makeSavedStateCurrent();
            if (mSurfaceTexture == null)
                Log.w(TAG, "getSurfaceTextureForDisplay called before ST created");
            return mSurfaceTexture;
        }
    }

    /**
     * Called from UI thread
     *
     * @return
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRecordingRequested;
        }
    }

    /**
     * Called from UI thread
     */
    public void startRecording() {
        Log.d(TAG, "Encoder: startRecording()");
        synchronized (mReadyForFrameFence) {
            mFrameNum = 0;
            mRecordingRequested = true;
        }
    }

    public void startEncodingThread() {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "CameraRecorder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    public void stopRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
    }

    private void handleStopRecording() {
        synchronized (mReadyForFrameFence) {
            mRecordingRequested = false;
            mVideoEncoder.drainEncoder(true);
            releaseEncoder();
        }
    }

    /**
     * Called on an "arbitrary thread"
     *
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // Pass SurfaceTexture to Encoding thread via Handler
        // Then Encode and display frame
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, surfaceTexture));
    }

    // GLSurfaceView.Renderer hooks

    /**
     * Called on Encoder thread
     *
     * @param surfaceTexture
     */
    private void handleFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (mReadyForFrameFence) {
            if (!mReadyForFrames) {
                Log.i(TAG, "Ignoring available frame, not ready");
                return;
            }

            if (!surfaceTexture.equals(mSurfaceTexture))
                Log.w(TAG, "SurfaceTexture from OnFrameAvailable does not match saved SurfaceTexture!");
            mInputWindowSurface.makeCurrent();
            mSurfaceTexture.updateTexImage();

            if (mRecordingRequested) {
                //Log.i(TAG, "encoding frame");
                mVideoEncoder.drainEncoder(false);
                surfaceTexture.getTransformMatrix(mTransform);
                mFullScreen.drawFrame(mTextureId, mTransform);

                mInputWindowSurface.setPresentationTime(mSurfaceTexture.getTimestamp());
                mInputWindowSurface.swapBuffers();
            }
        }

        //Log.i(TAG+"-SurfaceTexture", "pre requestRender");
        // Signal GLSurfaceView to render
        mDisplayView.requestRender();

    }

    /**
     * The GLSurfaceView.Renderer calls here after creating a
     * new texture in the display rendering context. Use the
     * created textureId to create a SurfaceTexture for
     * connection to the camera
     *
     * @param textureId
     */
    public void onSurfaceCreated(int textureId) {
        //The following happens on the GLSurfaceView renderer thread
        Log.i(TAG, "onSurfaceCreated. Saving EGL State");
        mEglSaver.saveEGLState();
        mEglSaver.makeNothingCurrent();
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_SURFACE_TEXTURE, new Integer(textureId)));
    }

    /**
     * Called on Encoder thread
     * TODO: Handle updatingSurfaceTexture EGLContext without recreating / restarting camera
     */
    private void handleSetSurfaceTexture(int textureId) {
        synchronized (mSurfaceTextureFence) {
            mEglSaver.makeSavedStateCurrent();  // Make display EGL Context current.
            prepareEncoder(mEglSaver.getSavedEGLContext(),
                    mRecorderConfig.getVideoWidth(),
                    mRecorderConfig.getVideoHeight(),
                    mRecorderConfig.getVideoBitrate(),
                    mRecorderConfig.getOuputFile());
            mTextureId = textureId;
            mSurfaceTexture = new SurfaceTexture(mTextureId);
            Log.i(TAG + "-SurfaceTexture", " SurfaceTexture created. pre setOnFrameAvailableListener");
            mSurfaceTexture.setOnFrameAvailableListener(this);
            openCamera(mRecorderConfig.getVideoWidth(), mRecorderConfig.getVideoHeight());
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
            mReadyForFrames = true;
            mEglSaver.makeNothingCurrent();
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

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    /**
     * Called with the display EGLContext current, on Encoder thread
     *
     * @param sharedContext
     * @param width
     * @param height
     * @param bitRate
     * @param outputFile
     */
    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate,
                                File outputFile) {
        mVideoEncoder = new VideoEncoderCore(width, height, bitRate, outputFile);
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface());
        mInputWindowSurface.makeCurrent();

        mFullScreen = new FullFrameRect(Texture2dProgram.ProgramType.TEXTURE_EXT);

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
    }

    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     */
    private void openCamera(int desiredWidth, int desiredHeight) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

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
        Log.i(TAG, "Camera preview set: " + previewFacts);
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    public static class EncoderHandler extends Handler {
        private WeakReference<CameraRecorder> mWeakEncoder;

        public EncoderHandler(CameraRecorder encoder) {
            mWeakEncoder = new WeakReference<CameraRecorder>(encoder);
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

            CameraRecorder encoder = mWeakEncoder.get();
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
                    Looper.myLooper().quit();
                    break;
                case MSG_FRAME_AVAILABLE:
                    encoder.handleFrameAvailable((SurfaceTexture) obj);
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }
}
