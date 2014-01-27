package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import pro.dbro.ffmpegwrapper.FFmpegWrapper;

/**
 * Created by davidbrodsky on 1/23/14.
 */
//TODO: Proper track index management
public class FFmpegMuxer extends Muxer {
    private static final String TAG = "FFmpegMuxer";
    private static final boolean VERBOSE = false;

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
        super();
        mFFmpeg = new FFmpegWrapper();
        // Using FFmpeg as a muxer, the only option
        // we'd conceivably configure is the outputFormatName, right?
        // For now, hardcoded to "hls"
        mFFmpeg.prepareAVFormatContext(outputFile);
        mStarted = true;
        mCachedAudioPacket = new byte[1024];
    }

    public static FFmpegMuxer create(String outputFile, FORMAT format) {
        return new FFmpegMuxer(outputFile, format);
    }

    @Override
    public int addTrack(MediaFormat trackFormat) {
        super.addTrack(trackFormat);
        // With FFmpeg, we want to write the encoder's
        // BUFFER_FLAG_CODEC_CONFIG buffer directly
        // Whereas with MediaMuxer this call handles that.
        // TODO: Ensure addTrack isn't called more times than it should be...
        // TODO: Make an FFmpegWrapper API for this
        if (trackFormat.getString(MediaFormat.KEY_MIME).compareTo("video/avc") == 0)
            return 0;
        else
            return 1;
    }

    @Override
    public void start() {
        // Not needed
    }

    @Override
    public void stop() {
        mFFmpeg.finalizeAVFormatContext();
        mStarted = false;
    }

    @Override
    public void release() {
        // Not needed?
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void writeSampleData(int trackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        super.writeSampleData(trackIndex, encodedData, bufferInfo);

        if (((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)) {
            if (trackIndex == 0) {
                // Capture H.264 SPS + PPS Data
                if (VERBOSE) Log.i(TAG, "Capture SPS + PPS");
                captureH264MetaData(encodedData, bufferInfo);
            }
            return; // Don't write CODEC_CONFIG data as ordinary packet
        }

        if (trackIndex == 1) {
            // If Audio packet
            addAdtsToByteBuffer(encodedData, bufferInfo);
        }

        if (trackIndex == 0 && ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0)) {
            // Precede H.264 Keyframe with SPS + PPS Data
            if (VERBOSE) Log.i(TAG, "SPS + PPS pre-keyframe. PTS " + (bufferInfo.presentationTimeUs - 10) + " size: " + h264SPSandPPS.capacity());
            // FFmpeg's packet interleaver requires monotonically increasing PTS though it isn't meaningful for SPS/PPS packets
            if (bufferInfo.presentationTimeUs < PTS_PAD) bufferInfo.presentationTimeUs += PTS_PAD;
            mFFmpeg.writeAVPacketFromEncodedData(h264SPSandPPS, 1, 0, h264SPSandPPS.capacity(), bufferInfo.flags, (bufferInfo.presentationTimeUs - PTS_PAD));
        }

        // adjust the ByteBuffer values to match BufferInfo (not needed?)
        encodedData.position(bufferInfo.offset);
        encodedData.limit(bufferInfo.offset + bufferInfo.size);

        if (VERBOSE) Log.i(TAG, "PTS " + bufferInfo.presentationTimeUs + " size: " + bufferInfo.size + " " + (trackIndex == 0 ? "video " : "audio ") + (((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) ? " keyframe" : ""));
        mFFmpeg.writeAVPacketFromEncodedData(encodedData, (trackIndex == 0 ? 1 : 0), bufferInfo.offset, bufferInfo.size, bufferInfo.flags, bufferInfo.presentationTimeUs);

        if (allTracksFinished())
            mFFmpeg.finalizeAVFormatContext();
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
}
