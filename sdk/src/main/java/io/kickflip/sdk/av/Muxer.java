package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Created by davidbrodsky on 1/23/14.
 */
public interface Muxer {
    /**
     * Adds the specified track and returns the track index
     *
     * @param trackFormat MediaFormat of the track to add. Gotten from MediaCodec#dequeueOutputBuffer
     *                    when returned status is INFO_OUTPUT_FORMAT_CHANGED
     * @return index of track in output file
     */
    public int addTrack(MediaFormat trackFormat);

    public void start();

    public void stop();

    public void release();

    public boolean isStarted();

    public void writeSampleData(int trackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo);
}
