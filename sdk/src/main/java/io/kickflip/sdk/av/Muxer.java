package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by davidbrodsky on 1/23/14.
 */
public abstract class Muxer {
    private static final String TAG = "Muxer";

    public static enum FORMAT { MPEG4 }

    private final int mExpectedNumTracks = 2;           // TODO: Make this configurable?

    protected String mOutputPath;
    protected int mNumTracks;
    protected int mNumTracksFinished;

    protected Muxer(String outputPath){
        mOutputPath = checkNotNull(outputPath);
        mNumTracks = 0;
        mNumTracksFinished = 0;
    }

    public String getOutputPath(){
        return mOutputPath;
    }

    /**
     * Adds the specified track and returns the track index
     *
     * @param trackFormat MediaFormat of the track to add. Gotten from MediaCodec#dequeueOutputBuffer
     *                    when returned status is INFO_OUTPUT_FORMAT_CHANGED
     * @return index of track in output file
     */
    public int addTrack(MediaFormat trackFormat){
        mNumTracks++;
        return 0;
    }

    public void start(){
    }

    public void stop(){
    }

    public void release(){
    }

    public boolean isStarted(){
        return false;
    }

    public void writeSampleData(int trackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo){
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            signalEndOfTrack();
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
        }
    }

    protected boolean allTracksFinished(){
        return (mNumTracks == mNumTracksFinished);
    }

    protected boolean allTracksAdded(){
        return (mNumTracks == mExpectedNumTracks);
    }

    /**
     * Muxer will call this itself if it detects BUFFER_FLAG_END_OF_STREAM
     * in writeSampleData. However Video encoders with Surface input
     * are responsible for calling this themselves
     */
    public void signalEndOfTrack(){
        mNumTracksFinished++;
    }
}
