package io.kickflip.sdk.av;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class CameraSurfaceRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraSurfaceRenderer";
    private static final boolean VERBOSE = false;

    private CameraEncoder mVideoRecorder;

    private FullFrameRect mFullScreen;

    private final float[] mSTMatrix = new float[16];
    private int mTextureId;

    private boolean mRecordingEnabled;


    /**
     * Constructs CameraSurfaceRenderer.
     * <p>
     * @param recorder video encoder object
     */
    public CameraSurfaceRenderer(CameraEncoder recorder) {
        mVideoRecorder = recorder;

        mTextureId = -1;

        mRecordingEnabled = false;
    }

    /**
     * Notifies the renderer thread that the activity is pausing.
     * <p>
     * For best results, call this *after* disabling Camera preview.
     */
    /*
    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }
    */

    /**
     * Notifies the renderer that we want to stop or start recording.
     */
    public void changeRecordingState(boolean isRecording) {
        Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
        mRecordingEnabled = isRecording;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreen = new FullFrameRect(Texture2dProgram.ProgramType.TEXTURE_EXT);
        //mFullScreen = new FullFrameRect(Texture2dProgram.ProgramType.TEXTURE_EXT_BW);
        mTextureId = mFullScreen.createTextureObject();

        mVideoRecorder.onSurfaceCreated(mTextureId);
        /*
        // We're starting up or coming back.  Either way we've got a new EGLContext that will
        // need to be shared with the video encoder, so figure out if a recording is already
        // in progress.
        mRecordingEnabled = mVideoRecorder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
        }

        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        //
        // (In a previous version the TEXTURE_EXT_BW variant was enabled by a flag called
        // ROSE_COLORED_GLASSES, because the shader set the red channel and set everything
        // else to zero.)
        mFullScreen = new FullFrameRect(Texture2dProgram.ProgramType.TEXTURE_EXT);
        //mFullScreen = new FullScreen(Texture2dProgram.ProgramType.TEXTURE_EXT_BW);
        mTextureId = mFullScreen.createTextureObject();

        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        // Tell the UI thread to enable the camera preview.
        mEncoderHandler.sendMessage(mEncoderHandler.obtainMessage(
                CameraCaptureActivity.CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));
        */
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (VERBOSE) Log.d(TAG, "onDrawFrame tex=" + mTextureId);
        boolean showBox = false;
        /*
        // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
        // was there before.
        mSurfaceTexture.updateTexImage();

        // If the recording state is changing, take care of it here.  Ideally we wouldn't
        // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
        // makes it hard to do elsewhere.
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");
                    // start recording
                    mVideoRecorder.startRecording(new TextureMovieEncoder.VideoEncoderConfig(
                            mOutputFile, 640, 480, 1000000, EGL14.eglGetCurrentContext()));
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    Log.d(TAG, "RESUME recording");
                    mVideoRecorder.updateSharedContext(EGL14.eglGetCurrentContext());
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    Log.d(TAG, "STOP recording");
                    mVideoRecorder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }

        // Set the video encoder's texture name.  We only need to do this once, but in the
        // current implementation it has to happen after the video encoder is started, so
        // we just do it here.
        //
        // TODO: be less lame.
        mVideoRecorder.setTextureId(mTextureId);

        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
        mVideoRecorder.frameAvailable(mSurfaceTexture);

        */

        // Draw the video frame.
        if(mVideoRecorder.getSurfaceTextureForDisplay() != null){
            //Log.i(TAG, "Attempting to display frame");
            mVideoRecorder.getSurfaceTextureForDisplay().getTransformMatrix(mSTMatrix);
            mFullScreen.drawFrame(mTextureId, mSTMatrix);
        }

        /*

        // Draw a flashing box if we're recording.  This only appears on screen.
        showBox = (mRecordingStatus == RECORDING_ON);
        if (showBox && (++mFrameCount & 0x04) == 0) {
            drawBox();
        }
        */
    }

    /**
     * Draws a red box in the corner.
     */
    private void drawBox() {
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(0, 0, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }
}