package io.kickflip.sdk.av;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class CameraSurfaceRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraSurfaceRenderer";
    private static final boolean VERBOSE = false;

    private CameraEncoder mCameraEncoder;

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
        mCameraEncoder = recorder;

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
        mTextureId = mFullScreen.createTextureObject();

        mCameraEncoder.onSurfaceCreated(mTextureId);
        mFrameCount = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (VERBOSE){
            if(mFrameCount % 30 == 0){
                Log.d(TAG, "onDrawFrame tex=" + mTextureId);
                mCameraEncoder.logSavedEglState();
            }
        }

        if (mCurrentFilter != mNewFilter) {
            Filters.updateFilter(mFullScreen, mNewFilter);
            mCurrentFilter = mNewFilter;
        }

        if (mIncomingSizeUpdated) {
            mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
            Log.i(TAG, "setTexSize on display Texture");
        }

        // Draw the video frame.
        if(mCameraEncoder.isSurfaceTextureReadyForDisplay()){
            mCameraEncoder.getSurfaceTextureForDisplay().getTransformMatrix(mSTMatrix);
            mFullScreen.drawFrame(mTextureId, mSTMatrix);

        }

        // Draw a flashing box if we're recording.  This only appears on screen.
        showBox = (mCameraEncoder.isRecording());
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