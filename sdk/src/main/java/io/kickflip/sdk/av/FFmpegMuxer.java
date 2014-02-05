package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import pro.dbro.ffmpegwrapper.FFmpegWrapper;
import pro.dbro.ffmpegwrapper.FFmpegWrapper.AVOptions;

/**
 * Created by davidbrodsky on 1/23/14.
 */
//TODO: Remove hard-coded track indexes
public class FFmpegMuxer extends Muxer implements Runnable{
    private static final String TAG = "FFmpegMuxer";
    private static final boolean VERBOSE = false;

    // MuxerHandler message types
    private static final int MSG_WRITE_FRAME = 1;
    private static final int MSG_ADD_TRACK = 2;
    private static final int MSG_STOP = 3;

    private final Object mReadyFence = new Object();    // Synchronize muxing thread readiness
    private boolean mReady;                             // Is muxing thread ready
    private boolean mRunning;                           // Is muxer thread running
    private FFmpegHandler mHandler;
    private boolean mEncoderReleased;

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
    private static final int PTS_PAD = 10;      // Ensure some padding between SPS + PPS and keyframe packets to please FFmpeg
    private ByteBuffer h264SPSandPPS;
    private FFmpegWrapper mFFmpeg;
    private boolean mStarted;


    private FFmpegMuxer(String outputFile, FORMAT format) {
        super(outputFile, format);
        mReady = false;
        mFFmpeg = new FFmpegWrapper();

        AVOptions opts = new AVOptions();
        switch(format){
            case MPEG4:
                opts.outputFormatName = "mp4";
                break;
            case HLS:
                opts.outputFormatName = "hls";
                break;
            case RTMP:
                opts.outputFormatName = "flv";
                break;
            default:
                throw new IllegalArgumentException("Unrecognized format!");
        }

        mFFmpeg.setAVOptions(opts);
        mStarted = false;
        mEncoderReleased = false;

        if(formatRequiresADTS())
            mCachedAudioPacket = new byte[1024];

        startMuxingThread();
    }

    public static FFmpegMuxer create(String outputFile, FORMAT format) {
        return new FFmpegMuxer(outputFile, format);
    }

    @Override
    public int addTrack(MediaFormat trackFormat) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_TRACK, trackFormat));
        // With FFmpeg, we want to write the encoder's
        // BUFFER_FLAG_CODEC_CONFIG buffer directly via writeSampleData
        // Whereas with MediaMuxer this call handles that.
        // TODO: Ensure addTrack isn't called more times than it should be...
        // TODO: Make an FFmpegWrapper API that sets mVideo/AudioTrackIndex instead of hard-code
        if (trackFormat.getString(MediaFormat.KEY_MIME).compareTo("video/avc") == 0)
            return mVideoTrackIndex;
        else
            return mAudioTrackIndex;
    }

    public void handleAddTrack(MediaFormat trackFormat){
        super.addTrack(trackFormat);
        if(!mStarted){
            mFFmpeg.prepareAVFormatContext(getOutputPath());
            mStarted = true;
        }
    }

    @Override
    public void onEncoderReleased(){
        // Technically should be synchronized
        mEncoderReleased = true;
    }

    @Override
    public void release() {
        // Not needed?
    }

    /**
     * Shutdown this Muxer
     * Must be called from Muxer thread
     */
    private void shutdown(){
        mStarted = false;
        Looper.myLooper().quit();
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void writeSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_WRITE_FRAME,
                new WritePacketData(encoder, trackIndex, bufferIndex, encodedData, bufferInfo)));
    }

    public void handleWriteSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        super.writeSampleData(encoder, trackIndex, bufferIndex, encodedData, bufferInfo);

        if (((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)) {
            if (trackIndex == 0) {
                // Capture H.264 SPS + PPS Data
                if (VERBOSE) Log.i(TAG, "Capture SPS + PPS");
                captureH264MetaData(encodedData, bufferInfo);
            }
            return; // Don't write CODEC_CONFIG data as ordinary packet
        }

        if (trackIndex == mAudioTrackIndex && formatRequiresADTS()) {
            // If Audio packet requires ADTS, add it
            addAdtsToByteBuffer(encodedData, bufferInfo);
        }

        if (trackIndex == mVideoTrackIndex && ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) && h264SPSandPPS != null) {
            // Precede H.264 Keyframe with SPS + PPS Data
            if (VERBOSE) Log.i(TAG, "SPS + PPS pre-keyframe. PTS " + (bufferInfo.presentationTimeUs - 10) + " size: " + h264SPSandPPS.capacity());
            // FFmpeg's packet interleaver requires monotonically increasing PTS though it isn't meaningful for SPS/PPS packets
            if (bufferInfo.presentationTimeUs < PTS_PAD) bufferInfo.presentationTimeUs += PTS_PAD;
            mFFmpeg.writeAVPacketFromEncodedData(h264SPSandPPS, 1, 0, h264SPSandPPS.capacity(), bufferInfo.flags, (bufferInfo.presentationTimeUs - PTS_PAD));
        }

        // adjust the ByteBuffer values to match BufferInfo (not needed?)
        encodedData.position(bufferInfo.offset);
        encodedData.limit(bufferInfo.offset + bufferInfo.size);

        if (VERBOSE) Log.i(TAG, "PTS " + bufferInfo.presentationTimeUs + " size: " + bufferInfo.size + " " + (trackIndex == mVideoTrackIndex ? "video " : "audio ") + (((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) ? " keyframe" : ""));
        mFFmpeg.writeAVPacketFromEncodedData(encodedData, (trackIndex == mVideoTrackIndex ? 1 : 0), bufferInfo.offset, bufferInfo.size, bufferInfo.flags, bufferInfo.presentationTimeUs);

        // If a stop request was received
        // It's possible the encoder is no longer started, and
        // may even be released
        if(!mEncoderReleased)
            encoder.releaseOutputBuffer(bufferIndex, false);

        if (allTracksFinished()){
            mFFmpeg.finalizeAVFormatContext();
            shutdown();
        }
    }

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
        h264SPSandPPS = ByteBuffer.allocateDirect(bufferInfo.size);
        byte[] videoConfig = new byte[bufferInfo.size];
        encodedData.get(videoConfig, 0, bufferInfo.size);
        encodedData.position(bufferInfo.offset);
        encodedData.put(videoConfig, 0, bufferInfo.size);
        h264SPSandPPS.put(videoConfig, 0, bufferInfo.size);
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

    private void startMuxingThread(){
        synchronized (mReadyFence){
            if(mRunning) {
                Log.w(TAG, "Muxing thread running when start requested");
                return;
            }
            mRunning = true;
            new Thread(this, "FFmpeg").start();
            while(!mReady){
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
        synchronized (mReadyFence){
            mHandler = new FFmpegHandler(this);
            mReady = true;
            mReadyFence.notify();
        }

        Looper.loop();

        synchronized (mReadyFence){
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
        public void handleMessage(Message inputMessage){
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            FFmpegMuxer muxer = mWeakMuxer.get();
            if(muxer == null){
                Log.w(TAG, "FFmpegHandler.handleMessage: muxer is null");
                return;
            }

            switch(what){
                case MSG_ADD_TRACK:
                    muxer.handleAddTrack((MediaFormat) obj);
                    break;
                case MSG_WRITE_FRAME:
                    WritePacketData data = (WritePacketData) obj;
                    muxer.handleWriteSampleData(data.mEncoder,
                            data.mTrackIndex,
                            data.mBufferIndex,
                            data.mData,
                            data.mBufferInfo);
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

        public MediaCodec mEncoder;
        public int mTrackIndex;
        public int mBufferIndex;
        public ByteBuffer mData;
        public MediaCodec.BufferInfo mBufferInfo;

        public WritePacketData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer data, MediaCodec.BufferInfo bufferInfo){
            mEncoder = encoder;
            mTrackIndex = trackIndex;
            mBufferIndex = bufferIndex;
            mData = data;
            mBufferInfo = bufferInfo;
        }
    }
}
