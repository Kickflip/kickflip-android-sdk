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
 * This class essentially represents a sizezble sprite that will be rendered with
 * a texture, usually from an external source like the camera or video decoder.
 *
 * Placeholder. Not yet implemented.
 * @hide
 */
public class SizeableFrameRect{
    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.RECTANGLE);
    private Texture2dProgram mProgram;

    private static final int SIZEOF_FLOAT = 4;

    private static final float[] IDENTITY_MATRIX = new float[16];

    private static float TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };
    private static final FloatBuffer TEX_COORDS_BUF = GlUtil.createFloatBuffer(TEX_COORDS);
    private static final int TEX_COORDS_STRIDE = 2 * SIZEOF_FLOAT;


    /**
     * Prepares the object.
     *
     * @param program The program to use.  FullFrameRect takes ownership, and will release
     *     the program when no longer needed.
     */
    public SizeableFrameRect(Texture2dProgram program, float[] texCoords) {
        mProgram = program;
        TEX_COORDS = texCoords;

        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
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
     * Draws a rectangle in an area defined by TEX_COORDS
     */
    public void drawFrame(int textureId, float[] texMatrix) {
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        mProgram.draw(IDENTITY_MATRIX, mRectDrawable.getVertexArray(), 0,
                mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix, TEX_COORDS_BUF, textureId, TEX_COORDS_STRIDE);
    }

    /**
     * Pass touch event down to the
     * texture's shader program
     * @param ev
     */
    public void handleTouchEvent(MotionEvent ev){
        mProgram.handleTouchEvent(ev);
    }
}