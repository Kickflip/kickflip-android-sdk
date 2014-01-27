package io.kickflip.sdk.av;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

import java.io.File;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by davidbrodsky on 1/22/14.
 */
public class RecorderConfig {

    private Muxer mMuxer;
    private final VideoEncoderConfig mVideoConfig;
    private final AudioEncoderConfig mAudioConfig;

    public RecorderConfig(Muxer muxer, VideoEncoderConfig videoConfig, AudioEncoderConfig audioConfig) {
        mVideoConfig = checkNotNull(videoConfig);
        mAudioConfig = checkNotNull(audioConfig);

        mMuxer = checkNotNull(muxer);
    }

    public Muxer getMuxer(){
        return mMuxer;
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

        public Builder(File outputFile){
            //this(AndroidMuxer.create(outputFile.getAbsolutePath(), Muxer.FORMAT.MPEG4));
            this(FFmpegMuxer.create(checkNotNull(outputFile.getAbsolutePath()), Muxer.FORMAT.MPEG4));
        }

        public Builder(Muxer muxer) {
            mWidth = 1280;
            mHeight = 720;
            mVideoBitrate = 2 * 1000 * 1000;

            mAudioSamplerate = 44100;
            mAudioBitrate = 96 * 1000;
            mNumAudioChannels = 1;

            mMuxer = checkNotNull(muxer);
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
            return new RecorderConfig(mMuxer,
                    new VideoEncoderConfig(mWidth, mHeight, mVideoBitrate),
                    new AudioEncoderConfig(mNumAudioChannels, mAudioSamplerate, mAudioBitrate));
        }


    }
}
