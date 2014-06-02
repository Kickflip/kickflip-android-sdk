package io.kickflip.sdk.av;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;

import static io.kickflip.sdk.Kickflip.isKitKat;

/**
 * @hide
 */
public abstract class AndroidEncoder {
    private final static String TAG = "AndroidEncoder";
    private final static boolean VERBOSE = false;

    protected Muxer mMuxer;
    protected MediaCodec mEncoder;
    protected MediaCodec.BufferInfo mBufferInfo;
    protected int mTrackIndex;
    protected volatile boolean mForceEos = false;

    public void forceEos() {
        mForceEos = true;
    }

    public void release(){
        if(mMuxer != null)
            mMuxer.onEncoderReleased(mTrackIndex);
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
            if (VERBOSE) Log.i(TAG, "Released encoder");
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void adjustBitrate(int targetBitrate){
        if(isKitKat() && mEncoder != null){
            Bundle bitrate = new Bundle();
            bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, targetBitrate);
            mEncoder.setParameters(bitrate);
        }else if (!isKitKat()) {
            Log.w(TAG, "Ignoring adjustVideoBitrate call. This functionality is only available on Android API 19+");
        }
    }

    public void drainEncoder(boolean endOfStream) {
        if (endOfStream) {
            if (isSurfaceInputEncoder()) {
                Log.i(TAG, "final video drain");
            } else {
                Log.i(TAG, "final audio drain");
            }
        }
        synchronized (mMuxer){
            final int TIMEOUT_USEC = 1000;
            if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ") track: " + mTrackIndex);

            if (endOfStream) {
                if (VERBOSE) Log.d(TAG, "sending EOS to encoder for track " + mTrackIndex);
                if(isSurfaceInputEncoder()){
                    if (VERBOSE) Log.i(TAG, "signalEndOfInputStream for track " + mTrackIndex);
                    mEncoder.signalEndOfInputStream();
                }
            }

            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
            while (true) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (!endOfStream) {
                        break;      // out of while
                    } else {
                        if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                    }
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                 } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // should happen before receiving buffers, and should only happen once
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "encoder output format changed: " + newFormat);

                    // now that we have the Magic Goodies, start the muxer
                    mTrackIndex = mMuxer.addTrack(newFormat);
                    // Muxer is responsible for starting/stopping itself
                    // based on knowledge of expected # tracks
                } else if (encoderStatus < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }

                    if (mBufferInfo.size >= 0) {    // Allow zero length buffer for purpose of sending 0 size video EOS Flag
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                        if (mForceEos) {
                            mBufferInfo.flags = mBufferInfo.flags | MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                            Log.i(TAG, "Forcing EOS");
                        }
                        // It is the muxer's responsibility to release encodedData
                        mMuxer.writeSampleData(mEncoder, mTrackIndex, encoderStatus, encodedData, mBufferInfo);
                        if (VERBOSE) {
                            Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, \t ts=" +
                                    mBufferInfo.presentationTimeUs + "track " + mTrackIndex);
                        }
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (!endOfStream) {
                            Log.w(TAG, "reached end of stream unexpectedly");
                        } else {
                            if (VERBOSE) Log.d(TAG, "end of stream reached for track " + mTrackIndex);
                        }
                        break;      // out of while
                    }
                }
            }
        }
    }

    protected abstract boolean isSurfaceInputEncoder();
}
