package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base Muxer class for interaction with MediaCodec based
 * encoders
 */
public abstract class Muxer {
    private static final String TAG = "Muxer";

    public static enum FORMAT { MPEG4, HLS, RTMP }

    private final int mExpectedNumTracks = 2;           // TODO: Make this configurable?

    protected FORMAT mFormat;
    protected String mOutputPath;
    protected int mNumTracks;
    protected int mNumTracksFinished;

    protected Muxer(String outputPath, FORMAT format){
        mOutputPath = checkNotNull(outputPath);
        mFormat = format;
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

    /* Deprecated
       Starting and stopping should be handled internally
       by the muxer based on knowledge of expected number of tracks,
       and the number of tracks already added / finalized
    public void start(){
    }

    public void stop(){
    }
    */

    /**
     * Called by the hosting Encoder
     * to notify the Muxer that it should no
     * longer assume the Encoder resources are available.
     *
     */
    public void onEncoderReleased(){
    }

    public void release(){
    }

    public boolean isStarted(){
        return false;
    }

    /**
     * Write the MediaCodec output buffer. This method <b>must</b>
     * be overridden by subclasses to release encodedData, transferring
     * ownership back to encoder, by calling encoder.releaseOutputBuffer(bufferIndex, false);
     *
     * @param trackIndex
     * @param encodedData
     * @param bufferInfo
     */
    public void writeSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo){
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

    /**
     * Does this Muxer's format require AAC ADTS headers?
     * see http://wiki.multimedia.cx/index.php?title=ADTS
     * @return
     */
    protected boolean formatRequiresADTS(){
        switch(mFormat){
            case MPEG4:
                break;
            case HLS:
                return true;
            case RTMP:
                break;
        }
        return false;
    }
}
