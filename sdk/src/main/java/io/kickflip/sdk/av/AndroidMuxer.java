package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by davidbrodsky on 1/23/14.
 */
public class AndroidMuxer implements Muxer {
    private static final String TAG = "AndroidMuxer";
    private static final boolean VERBOSE = false;

    private MediaMuxer mMuxer;
    private boolean mStarted;

    private AndroidMuxer(String outputFile, int format){
        try {
            mMuxer = new MediaMuxer(outputFile, format);
        } catch (IOException e) {
            throw new RuntimeException("MediaMuxer creation failed", e);
        }
        mStarted = false;
    }

    public static AndroidMuxer create(String outputFile, int format) {
        return new AndroidMuxer(outputFile, format);
    }

    @Override
    public int addTrack(MediaFormat trackFormat) {
        if(mStarted)
            throw new RuntimeException("format changed twice");
        return mMuxer.addTrack(trackFormat);
    }

    @Override
    public void start() {
        mMuxer.start();
        mStarted = true;
    }

    @Override
    public void stop() {
        mMuxer.stop();
        mStarted = false;
    }

    @Override
    public void release() {
        mMuxer.release();
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void writeSampleData(int trackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // MediaMuxer gets the codec config info via the addTrack command
            if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            return;
        }

        if (!mStarted) {
            throw new RuntimeException("muxer hasn't started");
        }

        mMuxer.writeSampleData(trackIndex, encodedData, bufferInfo);
    }
}
