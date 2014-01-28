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

    private int mFrameCount;

    // Keep track of selected filters + relevant state
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;
    private int mCurrentFilter;
    private int mNewFilter;

    boolean showBox = false;


    /**
     * Constructs CameraSurfaceRenderer.
     * <p>
     * @param recorder video encoder object
     */
    public CameraSurfaceRenderer(CameraEncoder recorder) {
        mVideoRecorder = recorder;

        mTextureId = -1;
        mFrameCount = -1;

        RecorderConfig config = recorder.getConfig();
        mIncomingWidth = config.getVideoWidth();
        mIncomingHeight = config.getVideoHeight();
        mIncomingSizeUpdated = true;        // Force texture size update on next onDrawFrame

        mCurrentFilter = -1;
        mNewFilter = Filters.FILTER_NONE;

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
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        //mFullScreen = new FullFrameRect(Texture2dProgram.ProgramType.TEXTURE_EXT);
        //mFullScreen = new FullFrameRect(Texture2dProgram.ProgramType.TEXTURE_EXT_BW);
        mTextureId = mFullScreen.createTextureObject();

        mVideoRecorder.onSurfaceCreated(mTextureId);
        mFrameCount = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (VERBOSE) Log.d(TAG, "onDrawFrame tex=" + mTextureId);

        if (mCurrentFilter != mNewFilter) {
            //updateFilter();
            Filters.updateFilter(mFullScreen, mNewFilter);
            mCurrentFilter = mNewFilter;
        }

        if (mIncomingSizeUpdated) {
            mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
        }

        // Draw the video frame.
        if(mVideoRecorder.getSurfaceTextureForDisplay() != null){
            //Log.i(TAG, "Attempting to display frame");
            mVideoRecorder.getSurfaceTextureForDisplay().getTransformMatrix(mSTMatrix);
            mFullScreen.drawFrame(mTextureId, mSTMatrix);
        }

        // Draw a flashing box if we're recording.  This only appears on screen.
        showBox = (mVideoRecorder.isRecording());
        if (showBox && (++mFrameCount & 0x04) == 0) {
            drawBox();
        }
        mFrameCount++;

    }

    /**
     * Draws a red box in the corner.
     */
    private void drawBox() {
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(50, 50, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.7f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    /**
     * Changes the filter that we're applying to the camera preview.
     */
    public void changeFilterMode(int filter) {
        mNewFilter = filter;
    }

}