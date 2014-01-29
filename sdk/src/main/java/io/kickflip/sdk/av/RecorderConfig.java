package io.kickflip.sdk.av;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

import java.io.File;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by davidbrodsky on 1/22/14.
 */
public class RecorderConfig {

    private Muxer mMuxer;
    private final VideoEncoderConfig mVideoConfig;
    private final AudioEncoderConfig mAudioConfig;

    private final UUID mUUID;

    public RecorderConfig(){
        mVideoConfig = new VideoEncoderConfig(1280, 720, 2 * 1000 * 1000);
        mAudioConfig = new AudioEncoderConfig(1, 44100, 96 * 1000);

        mUUID = UUID.randomUUID();

        File rootDir = new File(Environment.getExternalStorageDirectory(), "Kickflip");
        File outputDir = new File(rootDir, mUUID.toString());
        File outputFile = new File(outputDir, String.format("kf_%d.m3u8", System.currentTimeMillis()));
        outputDir.mkdir();
        mMuxer = FFmpegMuxer.create(outputFile.getAbsolutePath(), Muxer.FORMAT.MPEG4);
    }

    public RecorderConfig(UUID uuid, Muxer muxer, VideoEncoderConfig videoConfig, AudioEncoderConfig audioConfig) {
        mVideoConfig = checkNotNull(videoConfig);
        mAudioConfig = checkNotNull(audioConfig);

        mMuxer = checkNotNull(muxer);
        mUUID = uuid;
    }

    public UUID getUUID(){
        return mUUID;
    }

    public Muxer getMuxer(){
        return mMuxer;
    }

    public String getOutputPath(){
        return mMuxer.getOutputPath();
    }

    public int getVideoWidth(){
        return mVideoConfig.getWidth();
    }

    public int getVideoHeight(){
        return mVideoConfig.getHeight();
    }

    public int getVideoBitrate(){
        return mVideoConfig.getmBitRate();
    }

    public int getNumAudioChannels(){
        return mAudioConfig.getNumChannels();
    }

    public int getAudioBitrate(){
        return mAudioConfig.getBitrate();
    }

    public int getAudioSamplerate(){
        return mAudioConfig.getSampleRate();
    }


    public static class Builder {
        private int mWidth;
        private int mHeight;
        private int mVideoBitrate;

        private int mAudioSamplerate;
        private int mAudioBitrate;
        private int mNumAudioChannels;

        private Muxer mMuxer;

        private UUID mUUID;

        /**
         * Use this builder to have the file structure automatically managed
         * by recording UUID
         * @param rootOutputDir recordings will be stored at <rootOutputDir>/<UUID>/
         */
        public Builder(File rootOutputDir){
            //TODO: Make this HLS / RTMP Agnostic
            setDefaults();
            mUUID = UUID.randomUUID();
            File outputDir = new File(rootOutputDir, mUUID.toString());
            outputDir.mkdir();
            File outputFile = new File(outputDir, "hls.m3u8");
            mMuxer = FFmpegMuxer.create(outputFile.getAbsolutePath(), Muxer.FORMAT.MPEG4);
        }

        /**
         * Use this builder to manage file hierarchy manually
         * or to provide your own Muxer
         * @param muxer
         */
        public Builder(Muxer muxer) {
            setDefaults();
            mMuxer = checkNotNull(muxer);
            mUUID = UUID.randomUUID();
        }

        private void setDefaults(){
            mWidth = 1280;
            mHeight = 720;
            mVideoBitrate = 2 * 1000 * 1000;

            mAudioSamplerate = 44100;
            mAudioBitrate = 96 * 1000;
            mNumAudioChannels = 1;
        }

        public Builder withMuxer(Muxer muxer) {
            mMuxer = checkNotNull(muxer);
            return this;
        }

        public Builder withVideoResolution(int width, int height) {
            mWidth = width;
            mHeight = height;
            return this;
        }

        public Builder withVideoBitrate(int bitrate) {
            mVideoBitrate = bitrate;
            return this;
        }

        public Builder withAudioSamplerate(int samplerate) {
            mAudioSamplerate = samplerate;
            return this;
        }

        public Builder withAudioBitrate(int bitrate) {
            mAudioBitrate = bitrate;
            return this;
        }

        public Builder withAudioChannels(int numChannels){
            checkArgument(numChannels == 0 || numChannels == 1);
            mNumAudioChannels = numChannels;
            return this;
        }

        public RecorderConfig build() {
            return new RecorderConfig(mUUID, mMuxer,
                    new VideoEncoderConfig(mWidth, mHeight, mVideoBitrate),
                    new AudioEncoderConfig(mNumAudioChannels, mAudioSamplerate, mAudioBitrate));
        }


    }
}
