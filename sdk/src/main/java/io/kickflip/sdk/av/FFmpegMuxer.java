package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.util.Log;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;

import net.openwatch.ffmpegwrapper.FFmpegWrapper;

/**
 * Created by davidbrodsky on 1/23/14.
 *
 * @hide
 */
//TODO: Remove hard-coded track indexes
//      Remove 2 track assumption
public class FFmpegMuxer extends Muxer implements Runnable {
    private static final String TAG = "FFmpegMuxer";
    private static final boolean VERBOSE = false;        // Lots of logging
    private static final boolean TRACE = false;           // Systrace logs
    private static final boolean DEBUG_PKTS = false;     // Write each raw packet to file

    // MuxerHandler message types
    private static final int MSG_WRITE_FRAME = 1;
    private static final int MSG_ADD_TRACK = 2;
    private static final int MSG_FORCE_SHUTDOWN = 3;

    private final Object mReadyFence = new Object();    // Synchronize muxing thread readiness
    private boolean mReady;                             // Is muxing thread ready
    private boolean mRunning;                           // Is muxer thread running
    private FFmpegHandler mHandler;
    private final Object mEncoderReleasedSync = new Object();
    private boolean mEncoderReleased;                   // TODO: Account for both encoders

    private final int mVideoTrackIndex = 0;
    private final int mAudioTrackIndex = 1;

    // Related to crafting ADTS headers
    private final int ADTS_LENGTH = 7;          // ADTS Header length (bytes)
    private final int profile = 2;              // AAC LC
    private int freqIdx = 4;                    // 44.1KHz
    private int chanCfg = 1;                    // MPEG-4 Audio Channel Configuration. 1 Channel front-center
    private int mInPacketSize;                  // Pre ADTS Header
    private int mOutPacketSize;                 // Post ADTS Header
    private byte[] mCachedAudioPacket;

    // Related to extracting H264 SPS + PPS from MediaCodec
    private ByteBuffer mH264Keyframe;
    private int mH264MetaSize = 0;                   // Size of SPS + PPS data
    private FFmpegWrapper mFFmpeg;
    private boolean mStarted;

        // Queue encoded buffers when muxing to stream
        ArrayList<ArrayDeque<ByteBuffer>> mMuxerInputQueue;

        private FFmpegMuxer(String outputFile, FORMAT format) {
            super(outputFile, format);
            mReady = false;
            mFFmpeg = new FFmpegWrapper();

        FFmpegWrapper.AVOptions opts = new FFmpegWrapper.AVOptions();
        switch (mFormat) {
            case MPEG4:
                opts.outputFormatName = "mp4";
                break;
            case HLS:
                opts.outputFormatName = "hls";
                break;
            default:
                throw new IllegalArgumentException("Unrecognized format!");
        }

        mFFmpeg.setAVOptions(opts);
        mStarted = false;
        mEncoderReleased = false;

        if (formatRequiresADTS())
            mCachedAudioPacket = new byte[1024];

        if (formatRequiresBuffering()) {
            mMuxerInputQueue = new ArrayList<>();
            startMuxingThread();
        } else
            mReady = true;
    }

    public static FFmpegMuxer create(String outputFile, FORMAT format) {
        return new FFmpegMuxer(outputFile, format);
    }

    @Override
    public int addTrack(MediaFormat trackFormat) {
        // With FFmpeg, we want to write the encoder's
        // BUFFER_FLAG_CODEC_CONFIG buffer directly via writeSampleData
        // Whereas with MediaMuxer this call handles that.
        // TODO: Ensure addTrack isn't called more times than it should be...
        // TODO: Make an FFmpegWrapper API that sets mVideo/AudioTrackIndex instead of hard-code
        int trackIndex;
        if (trackFormat.getString(MediaFormat.KEY_MIME).compareTo("video/avc") == 0)
            trackIndex = mVideoTrackIndex;
        else
            trackIndex = mAudioTrackIndex;

        if (formatRequiresBuffering()) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_TRACK, trackFormat));
            synchronized (mMuxerInputQueue) {
                while (mMuxerInputQueue.size() < trackIndex + 1)
                    mMuxerInputQueue.add(new ArrayDeque<ByteBuffer>());
            }
        } else {
            handleAddTrack(trackFormat);
        }
        return trackIndex;
    }

    public void handleAddTrack(MediaFormat trackFormat) {
        super.addTrack(trackFormat);
        if (!mStarted) {
            Log.i(TAG, "PrepareAVFormatContext for path " + getOutputPath());
            mFFmpeg.prepareAVFormatContext(getOutputPath());
            mStarted = true;
        }
    }

    @Override
    public void onEncoderReleased(int trackIndex) {
        // For now assume both tracks will be
        // released in close proximity
        synchronized (mEncoderReleasedSync) {
            mEncoderReleased = true;
        }
    }

    /**
     * Shutdown this Muxer
     * Must be called from Muxer thread
     */
    private void shutdown() {
        mStarted = false;
        release();
        if (formatRequiresBuffering())
            Looper.myLooper().quit();
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void writeSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        synchronized (mReadyFence) {
            if (mReady) {
                ByteBuffer muxerInput;
                if (formatRequiresBuffering()) {
                    // Copy encodedData into another ByteBuffer, recycling if possible
                    synchronized (mMuxerInputQueue) {
                        muxerInput = mMuxerInputQueue.get(trackIndex).isEmpty() ?
                                ByteBuffer.allocateDirect(encodedData.capacity()) : mMuxerInputQueue.get(trackIndex).remove();
                    }
                    muxerInput.put(encodedData);
                    muxerInput.position(0);
                    encoder.releaseOutputBuffer(bufferIndex, false);
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_WRITE_FRAME,
                            new WritePacketData(encoder, trackIndex, bufferIndex, muxerInput, bufferInfo)));
                } else {
                    handleWriteSampleData(encoder, trackIndex, bufferIndex, encodedData, bufferInfo);
                }

            } else {
                Log.w(TAG, "Dropping frame because Muxer not ready!");
                releaseOutputBufer(encoder, encodedData, bufferIndex, trackIndex);
                if (formatRequiresBuffering())
                    encoder.releaseOutputBuffer(bufferIndex, false);
            }
        }
    }

    private void handleWriteSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        super.writeSampleData(encoder, trackIndex, bufferIndex, encodedData, bufferInfo);
        mPacketCount++;

        // Don't write the samples directly if they're CODEC_CONFIG data
        // Of if the muxer has already shutdown
        if (((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)) {
            if (VERBOSE) Log.i(TAG, "handling BUFFER_FLAG_CODEC_CONFIG for track " + trackIndex);
            if (trackIndex == mVideoTrackIndex) {
                // Capture H.264 SPS + PPS Data
                if (VERBOSE) Log.i(TAG, "Capture SPS + PPS");
                captureH264MetaData(encodedData, bufferInfo);
                releaseOutputBufer(encoder, encodedData, bufferIndex, trackIndex);
                return;
            } else {
                if (VERBOSE) Log.i(TAG, "Ignoring audio CODEC_CONFIG");
                releaseOutputBufer(encoder, encodedData, bufferIndex, trackIndex);
                return;
            }
        }

        if (trackIndex == mAudioTrackIndex && formatRequiresADTS()) {
            addAdtsToByteBuffer(encodedData, bufferInfo);
        }

        // adjust the ByteBuffer values to match BufferInfo (not needed?)
        encodedData.position(bufferInfo.offset);
        encodedData.limit(bufferInfo.offset + bufferInfo.size);

        bufferInfo.presentationTimeUs = getNextRelativePts(bufferInfo.presentationTimeUs, trackIndex);

        if (VERBOSE)
            Log.i(TAG, mPacketCount + " PTS " + bufferInfo.presentationTimeUs + " size: " + bufferInfo.size + " " + (trackIndex == mVideoTrackIndex ? "video " : "audio ") + (((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) ? "keyframe" : "") + (((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) ? " EOS" : ""));
        if (DEBUG_PKTS) writePacketToFile(encodedData, bufferInfo);

        if (!allTracksFinished()) {
            if (trackIndex == mVideoTrackIndex && ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0)) {
                packageH264Keyframe(encodedData, bufferInfo);
                mFFmpeg.writeAVPacketFromEncodedData(mH264Keyframe, 1, bufferInfo.offset, bufferInfo.size + mH264MetaSize, bufferInfo.flags, bufferInfo.presentationTimeUs);
            } else
                mFFmpeg.writeAVPacketFromEncodedData(encodedData, (trackIndex == mVideoTrackIndex ? 1 : 0), bufferInfo.offset, bufferInfo.size, bufferInfo.flags, bufferInfo.presentationTimeUs);
        }
        releaseOutputBufer(encoder, encodedData, bufferIndex, trackIndex);

        if (allTracksFinished()) {
            /*if (VERBOSE) */ Log.i(TAG, "Shutting down on last frame");
            handleForceStop();
        }
    }

    public void forceStop() {
        if (formatRequiresBuffering())
            mHandler.sendMessage(mHandler.obtainMessage(MSG_FORCE_SHUTDOWN));
        else
            handleForceStop();
    }

    private void handleForceStop() {
        Log.i(TAG, "Forcing Shutdown");
        mFFmpeg.finalizeAVFormatContext();
        shutdown();
    }

    private void releaseOutputBufer(MediaCodec encoder, ByteBuffer encodedData, int bufferIndex, int trackIndex) {
        synchronized (mEncoderReleasedSync) {
            if (!mEncoderReleased) {
                if (formatRequiresBuffering()) {
                    encodedData.clear();
                    synchronized (mMuxerInputQueue) {
                        mMuxerInputQueue.get(trackIndex).add(encodedData);
                    }
                } else {
                    encoder.releaseOutputBuffer(bufferIndex, false);
                }
            }
        }
    }

    //DEBUGGING USE ONLY
    private int mPacketCount = 0;

    private void writePacketToFile(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
        byte[] samples = new byte[bufferInfo.size];
        buffer.get(samples, bufferInfo.offset, bufferInfo.size);
        buffer.position(bufferInfo.offset);
        try {
            Files.write(samples, new File("/sdcard/Kickflip/packet_" + mPacketCount));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // END DEBUGGING USE ONLY

    /**
     * Should only be called once, when the encoder produces
     * an output buffer with the BUFFER_FLAG_CODEC_CONFIG flag.
     * For H264 output, this indicates the Sequence Parameter Set
     * and Picture Parameter Set are contained in the buffer.
     * These NAL units are required before every keyframe to ensure
     * playback is possible in a segmented stream.
     *
     * @param encodedData
     * @param bufferInfo
     */
    private void captureH264MetaData(ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        mH264MetaSize = bufferInfo.size;
        mH264Keyframe = ByteBuffer.allocateDirect(encodedData.capacity());
        byte[] videoConfig = new byte[bufferInfo.size];
        encodedData.get(videoConfig, bufferInfo.offset, bufferInfo.size);
        encodedData.position(bufferInfo.offset);
        encodedData.put(videoConfig, 0, bufferInfo.size);
        encodedData.position(bufferInfo.offset);
        mH264Keyframe.put(videoConfig, 0, bufferInfo.size);
    }

    /**
     * Adds the SPS + PPS data to the ByteBuffer containing a h264 keyframe
     *
     * @param encodedData
     * @param bufferInfo
     */
    private void packageH264Keyframe(ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        mH264Keyframe.position(mH264MetaSize);
        mH264Keyframe.put(encodedData); // BufferOverflow
    }

    private void addAdtsToByteBuffer(ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        mInPacketSize = bufferInfo.size;
        mOutPacketSize = mInPacketSize + ADTS_LENGTH;
        addAdtsToPacket(mCachedAudioPacket, mOutPacketSize);
        encodedData.get(mCachedAudioPacket, ADTS_LENGTH, mInPacketSize);
        encodedData.position(bufferInfo.offset);
        encodedData.limit(bufferInfo.offset + mOutPacketSize);
        try {
            encodedData.put(mCachedAudioPacket, 0, mOutPacketSize);
            encodedData.position(bufferInfo.offset);
            bufferInfo.size = mOutPacketSize;
        } catch (BufferOverflowException e) {
            Log.w(TAG, "BufferOverFlow adding ADTS header");
            encodedData.put(mCachedAudioPacket, 0, mOutPacketSize);        // drop last 7 bytes...
        }
    }

    /**
     * Add ADTS header at the beginning of each and every AAC packet.
     * This is needed as MediaCodec encoder generates a packet of raw
     * AAC data.
     * <p/>
     * Note the packetLen must count in the ADTS header itself.
     * See: http://wiki.multimedia.cx/index.php?title=ADTS
     * Also: http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio#Channel_Configurations
     */
    private void addAdtsToPacket(byte[] packet, int packetLen) {
        packet[0] = (byte) 0xFF;        // 11111111          = syncword
        packet[1] = (byte) 0xF9;        // 1111 1 00 1       = syncword MPEG-2 Layer CRC
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private void startMuxingThread() {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Muxing thread running when start requested");
                return;
            }
            mRunning = true;
            new Thread(this, "FFmpeg").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new FFmpegHandler(this);
            mReady = true;
            mReadyFence.notify();
        }

        Looper.loop();

        synchronized (mReadyFence) {
            mReady = false;
            mHandler = null;
        }
    }

    public static class FFmpegHandler extends Handler {
        private WeakReference<FFmpegMuxer> mWeakMuxer;

        public FFmpegHandler(FFmpegMuxer muxer) {
            mWeakMuxer = new WeakReference<FFmpegMuxer>(muxer);
        }

        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            FFmpegMuxer muxer = mWeakMuxer.get();
            if (muxer == null) {
                Log.w(TAG, "FFmpegHandler.handleMessage: muxer is null");
                return;
            }

            switch (what) {
                case MSG_ADD_TRACK:
                    if (TRACE) Trace.beginSection("addTrack");
                    muxer.handleAddTrack((MediaFormat) obj);
                    if (TRACE) Trace.endSection();
                    break;
                case MSG_WRITE_FRAME:
                    if (TRACE) Trace.beginSection("writeSampleData");
                    WritePacketData data = (WritePacketData) obj;
                    muxer.handleWriteSampleData(data.mEncoder,
                            data.mTrackIndex,
                            data.mBufferIndex,
                            data.mData,
                            data.getBufferInfo());
                    if (TRACE) Trace.endSection();
                    break;
                case MSG_FORCE_SHUTDOWN:
                    muxer.handleForceStop();
                    break;

                default:
                    throw new RuntimeException("Unexpected msg what=" + what);
            }
        }

    }

    /**
     * An object to encapsulate all the data
     * needed for writing a packet, for
     * posting to the Handler
     */
    public static class WritePacketData {

        private static MediaCodec.BufferInfo mBufferInfo;        // Used as singleton since muxer writes only one packet at a time

        public MediaCodec mEncoder;
        public int mTrackIndex;
        public int mBufferIndex;
        public ByteBuffer mData;
        public int offset;
        public int size;
        public long presentationTimeUs;
        public int flags;

        public WritePacketData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer data, MediaCodec.BufferInfo bufferInfo) {
            mEncoder = encoder;
            mTrackIndex = trackIndex;
            mBufferIndex = bufferIndex;
            mData = data;
            offset = bufferInfo.offset;
            size = bufferInfo.size;
            presentationTimeUs = bufferInfo.presentationTimeUs;
            flags = bufferInfo.flags;
        }

        public MediaCodec.BufferInfo getBufferInfo() {
            if (mBufferInfo == null)
                mBufferInfo = new MediaCodec.BufferInfo();
            mBufferInfo.set(offset, size, presentationTimeUs, flags);
            return mBufferInfo;
        }
    }
}
