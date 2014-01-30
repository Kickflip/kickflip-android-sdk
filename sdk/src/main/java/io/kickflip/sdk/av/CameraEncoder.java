package io.kickflip.sdk.av;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by davidbrodsky on 1/22/14.
 */
public class CameraEncoder implements SurfaceTexture.OnFrameAvailableListener, Runnable {
    private static final String TAG = "CameraEncoder";
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

    private int mCurrentFilter;
    private int mNewFilter;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;
    private EglStateSaver mEglSaver;

    private final Object mSurfaceTextureFence = new Object();   // guards mSurfaceTexture shared with GLSurfaceView.Renderer
    private SurfaceTexture mSurfaceTexture;
    private final Object mReadyForFrameFence = new Object();    // guards mReadyForFrames/mRecordingRequested
    private boolean mReadyForFrames;                            // Is the SurfaceTexture et all created
    private boolean mRecordingRequested;                        // Is Recording desired
    private final Object mReadyFence = new Object();            // guards ready/running
    private boolean mReady;                                     // mHandler created on Encoder thread
    private boolean mRunning;                                   // Encoder thread running

    private boolean mEncodedFirstFrame;
    private long mStartTimeNs;

    private GLSurfaceView mDisplayView;
    private CameraSurfaceRenderer mDisplayRenderer;

    private int mCurrentCamera;
    private int mDesiredCamera;

    public CameraEncoder(RecorderConfig config) {
        mEncodedFirstFrame = false;
        mReadyForFrames = false;
        mRecordingRequested = false;

        mCurrentCamera = -1;
        mDesiredCamera = Camera.CameraInfo.CAMERA_FACING_BACK;

        mCurrentFilter = -1;
        mNewFilter = Filters.FILTER_NONE;

        mRecorderConfig = checkNotNull(config);
        mEglSaver = new EglStateSaver();
        startEncodingThread();
    }

    public RecorderConfig getConfig(){
        return mRecorderConfig;
    }

    public void requestCamera(int camera){
        if(Camera.getNumberOfCameras() == 1){
            Log.w(TAG, "Ignoring requestCamera: only one device camera available.");
            return;
        }
        mDesiredCamera = camera;
        if(mCamera != null && mDesiredCamera != mCurrentCamera){
            // Hot swap camera
            releaseCamera();
            openCamera(mRecorderConfig.getVideoWidth(), mRecorderConfig.getVideoHeight(), mDesiredCamera);
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
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

    public void setPreviewDisplay(GLSurfaceView display) {
        checkNotNull(display);
        mDisplayRenderer = new CameraSurfaceRenderer(this);
        // Prep GLSurfaceView and attach Renderer
        display.setEGLContextClientVersion(2);
        display.setRenderer(mDisplayRenderer);
        display.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        display.setPreserveEGLContextOnPause(true);
        mDisplayView = display;
    }

    /**
     * Called from GLSurfaceView.Renderer thread
     *
     * @return The SurfaceTexture containing the camera frame to display. The
     *          display EGLContext is current on the calling thread
     *          when this call completes
     */
    public SurfaceTexture getSurfaceTextureForDisplay() {
        synchronized (mSurfaceTextureFence) {
            if (mSurfaceTexture == null)
                Log.w(TAG, "getSurfaceTextureForDisplay called before ST created");
            else{
                mEglSaver.makeSavedStateCurrent();
            }
            return mSurfaceTexture;
        }
    }

    /**
     * Apply a filter to the camera input
     * TODO: Apply to display and encoder
     * @param filter
     */
    public void applyFilter(int filter){
        checkArgument(filter >= 0 && filter <= 7);
        mDisplayRenderer.changeFilterMode(filter);
        synchronized (mReadyForFrameFence) {
            mNewFilter = filter;
        }
    }

    /**
     * Called from UI thread
     *
     * @return is the CameraEncoder in the recording state
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
        synchronized (mReadyForFrameFence) {
            mFrameNum = 0;
            mRecordingRequested = true;
        }
    }

    private void startEncodingThread() {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
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

    private void handleStopRecording() {
        synchronized (mReadyForFrameFence) {
            mRecordingRequested = false;
            mVideoEncoder.drainEncoder(true);
            releaseEncoder();
            releaseCamera();
        }
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
                if (mCurrentFilter != mNewFilter) {
                    Filters.updateFilter(mFullScreen, mNewFilter);
                    mCurrentFilter = mNewFilter;
                }
                surfaceTexture.getTransformMatrix(mTransform);
                mFullScreen.drawFrame(mTextureId, mTransform);
                if(!mEncodedFirstFrame){
                    mStartTimeNs = mSurfaceTexture.getTimestamp();
                    mEncodedFirstFrame = true;
                }
                mInputWindowSurface.setPresentationTime(mSurfaceTexture.getTimestamp() - mStartTimeNs);
                mInputWindowSurface.swapBuffers();
            }
        }

        // Signal GLSurfaceView to render
        mDisplayView.requestRender();

    }


    /**
     * The GLSurfaceView.Renderer calls here after creating a
     * new texture in the display rendering context. Use the
     * created textureId to create a SurfaceTexture for
     * connection to the camera
     *
     * @param textureId the id of the texture bound to the new display surface
     */
    public void onSurfaceCreated(int textureId) {
        //The following happens on the GLSurfaceView renderer thread
        Log.i(TAG, "onSurfaceCreated. Saving EGL State");
        mEglSaver.saveEGLState();
        mEglSaver.makeNothingCurrent();
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_SURFACE_TEXTURE, textureId));
    }

    /**
     * Called on Encoder thread
     */
    private void handleSetSurfaceTexture(int textureId) {
        synchronized (mSurfaceTextureFence) {
            if(mSurfaceTexture != null){
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
                //mFullScreen = new FullFrameRect(Texture2dProgram.ProgramType.TEXTURE_EXT);
                mFullScreen = new FullFrameRect(
                        new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
                mSurfaceTexture.attachToGLContext(mTextureId);
                mEglSaver.makeNothingCurrent();
            }else{
                // We're setting up the intial SurfaceTexure pre-recording
                mEglSaver.makeSavedStateCurrent();  // Make display EGL Context current.
                prepareEncoder(mEglSaver.getSavedEGLContext(),
                        mRecorderConfig.getVideoWidth(),
                        mRecorderConfig.getVideoHeight(),
                        mRecorderConfig.getVideoBitrate(),
                        mRecorderConfig.getMuxer());
                mTextureId = textureId;
                mSurfaceTexture = new SurfaceTexture(mTextureId);
                Log.i(TAG + "-SurfaceTexture", " SurfaceTexture created. pre setOnFrameAvailableListener");
                mSurfaceTexture.setOnFrameAvailableListener(this);
                openCamera(mRecorderConfig.getVideoWidth(), mRecorderConfig.getVideoHeight(), mDesiredCamera);
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
     * @param sharedContext The display EGLContext to be shared with the Encoder Surface's context.
     * @param width the desired width of the encoder's video output
     * @param height the desired height of the encoder's video output
     * @param bitRate the desired bitrate of the video encoder
     * @param muxer the desired output muxer
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
        while(!triedAllCameras){
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == targetCameraType) {
                    Log.i(TAG, "Trying to open camera " + i);
                    mCamera = Camera.open(i);
                    mCurrentCamera = targetCameraType;
                    break cameraLoop;
                }
            }
            if(mCamera == null){
                if(targetCameraType == requestedCameraType)
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
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            if (VERBOSE) Log.d(TAG, "releasing camera");
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    public static class EncoderHandler extends Handler {
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
