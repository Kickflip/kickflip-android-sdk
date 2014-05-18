/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.kickflip.sdk.av;

import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.FloatBuffer;

/**
 * This class essentially represents a viewport-sized sprite that will be rendered with
 * a texture, usually from an external source like the camera or video decoder.
 *
 * @hide
 */
public class FullFrameRect {
    public static enum SCREEN_ROTATION {LANDSCAPE, VERTICAL, UPSIDEDOWN_LANDSCAPE, UPSIDEDOWN_VERTICAL}

    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
    private Texture2dProgram mProgram;
    private final Object mDrawLock = new Object();

    private static final int SIZEOF_FLOAT = 4;

    private float[] IDENTITY_MATRIX = new float[16];

    private static final float TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };
    private static final FloatBuffer TEX_COORDS_BUF = GlUtil.createFloatBuffer(TEX_COORDS);
    private static final int TEX_COORDS_STRIDE = 2 * SIZEOF_FLOAT;

    private boolean mCorrectVerticalVideo = false;
    private boolean mScaleToFit;
    private SCREEN_ROTATION requestedOrientation = SCREEN_ROTATION.LANDSCAPE;


    /**
     * Prepares the object.
     *
     * @param program The program to use.  FullFrameRect takes ownership, and will release
     *                the program when no longer needed.
     */
    public FullFrameRect(Texture2dProgram program) {
        mProgram = program;
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    /**
     * Adjust the MVP Matrix to rotate and crop the texture
     * to make vertical video appear upright
     */
    public void adjustForVerticalVideo(SCREEN_ROTATION orientation, boolean scaleToFit) {
        synchronized (mDrawLock) {
            mCorrectVerticalVideo = true;
            mScaleToFit = scaleToFit;
            requestedOrientation = orientation;
            Matrix.setIdentityM(IDENTITY_MATRIX, 0);
            switch (orientation) {
                case VERTICAL:
                    if (scaleToFit) {
                        Matrix.rotateM(IDENTITY_MATRIX, 0, -90, 0f, 0f, 1f);
                        Matrix.scaleM(IDENTITY_MATRIX, 0, 3.16f, 1.0f, 1f);
                    } else {
                        Matrix.scaleM(IDENTITY_MATRIX, 0, 0.316f, 1f, 1f);
                    }
                    break;
                case UPSIDEDOWN_LANDSCAPE:
                    if (scaleToFit) {
                        Matrix.rotateM(IDENTITY_MATRIX, 0, -180, 0f, 0f, 1f);
                    }
                    break;
                case UPSIDEDOWN_VERTICAL:
                    if (scaleToFit) {
                        Matrix.rotateM(IDENTITY_MATRIX, 0, 90, 0f, 0f, 1f);
                        Matrix.scaleM(IDENTITY_MATRIX, 0, 3.16f, 1.0f, 1f);
                    } else {
                        Matrix.scaleM(IDENTITY_MATRIX, 0, 0.316f, 1f, 1f);
                    }
                    break;
            }
        }
    }

    /**
     * Releases resources.
     */
    public void release() {
        if (mProgram != null) {
            mProgram.release();
            mProgram = null;
        }
    }

    /**
     * Returns the program currently in use.
     */
    public Texture2dProgram getProgram() {
        return mProgram;
    }

    /**
     * Changes the program.  The previous program will be released.
     */
    public void changeProgram(Texture2dProgram program) {
        mProgram.release();
        mProgram = program;
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    public int createTextureObject() {
        return mProgram.createTextureObject();
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */
    public void drawFrame(int textureId, float[] texMatrix) {
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        synchronized (mDrawLock) {
            if (mCorrectVerticalVideo && !mScaleToFit && (requestedOrientation == SCREEN_ROTATION.VERTICAL || requestedOrientation == SCREEN_ROTATION.UPSIDEDOWN_VERTICAL)) {
                Matrix.scaleM(texMatrix, 0, 0.316f, 1.0f, 1f);
            }
            mProgram.draw(IDENTITY_MATRIX, mRectDrawable.getVertexArray(), 0,
                    mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
                    mRectDrawable.getVertexStride(),
                    texMatrix, TEX_COORDS_BUF, textureId, TEX_COORDS_STRIDE);
        }
    }

    /**
     * Pass touch event down to the
     * texture's shader program
     *
     * @param ev
     */
    public void handleTouchEvent(MotionEvent ev) {
        mProgram.handleTouchEvent(ev);
    }
}