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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Some OpenGL utility functions.
 * @hide
 */
public class GlUtil {
    private static final String TAG = "GlUtil";

    private static final int SIZEOF_FLOAT = 4;


    private GlUtil() {}     // do not instantiate

    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     * <p>
     * Throws a RuntimeException if the location is invalid.
     */
    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    /**
     * Writes GL version info to the log.
     */
    public static void logVersionInfo() {
        Log.i(TAG, "vendor  : " + GLES20.glGetString(GLES20.GL_VENDOR));
        Log.i(TAG, "renderer: " + GLES20.glGetString(GLES20.GL_RENDERER));
        Log.i(TAG, "version : " + GLES20.glGetString(GLES20.GL_VERSION));

        if (false) {
            int[] values = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
            int majorVersion = values[0];
            GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
            int minorVersion = values[0];
            if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
                Log.i(TAG, "iversion: " + majorVersion + "." + minorVersion);
            }
        }
    }

    public static int createTextureWithTextContent (String text){
        // Create an empty, mutable bitmap
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        // get a canvas to paint over the bitmap
        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(0,0,255,0);

        // get a background image from resources
        // note the image format must match the bitmap format
//        Drawable background = context.getResources().getDrawable(R.drawable.background);
//        background.setBounds(0, 0, 256, 256);
//        background.draw(canvas); // draw the background to our bitmap

        // Draw the text
        Paint textPaint = new Paint();
        textPaint.setTextSize(32);
        textPaint.setAntiAlias(true);
        textPaint.setARGB(0xff, 0xff, 0xff, 0xff);
        // draw the text centered
        canvas.drawText(text, 16,112, textPaint);

        int[] textures = new int[1];

        //Generate one texture pointer...
        GLES20.glGenTextures(1, textures, 0);
        //...and bind it to our array
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        //Create Nearest Filtered Texture
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //Different possible texture parameters, e.g. GLES20.GL_CLAMP_TO_EDGE
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        //Alpha blending
        //GLES20.glEnable(GLES20.GL_BLEND);
        //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        //Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        //Clean up
        bitmap.recycle();

        return textures[0];
    }

    public static int createTextureFromImage(Context context, int resId){
        // Create an empty, mutable bitmap
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        // get a canvas to paint over the bitmap
        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(0,0,255,0);

        // get a background image from resources
        // note the image format must match the bitmap format
        Drawable background = context.getResources().getDrawable(resId);
        background.setBounds(0, 0, 256, 256);
        background.draw(canvas); // draw the background to our bitmap

        int[] textures = new int[1];

        //Generate one texture pointer...
        GLES20.glGenTextures(1, textures, 0);
        //...and bind it to our array
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        //Create Nearest Filtered Texture
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //Different possible texture parameters, e.g. GLES20.GL_CLAMP_TO_EDGE
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        //Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        //Clean up
        bitmap.recycle();

        return textures[0];
    }
}