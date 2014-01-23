package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

import pro.dbro.ffmpegwrapper.FFmpegWrapper;

/**
 * Created by davidbrodsky on 1/23/14.
 */
public class FFmpegMuxer implements Muxer {

    private FFmpegWrapper mFFmpeg;
    private boolean mStarted;

    private FFmpegMuxer(String outputFile, int format){
        mFFmpeg = new FFmpegWrapper();
        // Using FFmpeg as a muxer, the only option
        // we'd conceivably configure is the outputFormatName, right?
        // For now, hardcoded to "hls"
        mFFmpeg.prepareAVFormatContext(outputFile);
        mStarted = true;
    }

    public static FFmpegMuxer create(String outputFile, int format) {
        return new FFmpegMuxer(outputFile, format);
    }

    @Override
    public int addTrack(MediaFormat trackFormat) {
        // With FFmpeg, we want to write the encoder's
        // BUFFER_FLAG_CODEC_CONFIG buffer directly
        // Whereas with MediaMuxer this call handles that.
        // TODO: Ensure addTrack isn't called more times than it should be...
        // TODO: Make an FFmpegWrapper API for this
        if(trackFormat.getString(MediaFormat.KEY_MIME).compareTo("video/avc")==0)
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
        mFFmpeg.writeAVPacketFromEncodedData(encodedData, (trackIndex == 0 ? 1 : 0), bufferInfo.offset, bufferInfo.size, bufferInfo.flags, bufferInfo.presentationTimeUs);
    }
}
