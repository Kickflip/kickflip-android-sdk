package io.kickflip.sdk.av;

import android.os.Environment;

import java.io.File;

/**
 * Created by davidbrodsky on 1/22/14.
 */
public class RecorderConfig {

    private final File mOutputFile;
    private final VideoEncoderConfig mVideoConfig;

    public RecorderConfig(File outputFile, VideoEncoderConfig videoConfig) {
        mOutputFile = outputFile;
        mVideoConfig = videoConfig;
    }

    public File getOuputFile() {
        return mOutputFile;
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

    public static class Builder {
        private int mWidth;
        private int mHeight;
        private int mBitRate;
        private File mOutputFile;

        public Builder() {
            mWidth = 1280;
            mHeight = 720;
            mBitRate = 2 * 1000 * 1000;
            mOutputFile = new File(Environment.getExternalStorageDirectory(), "kftest.mp4");
        }

        public Builder withOuputFile(File outputFile) {
            mOutputFile = outputFile;
            return this;
        }

        public Builder withVideoResolution(int width, int height) {
            mWidth = width;
            mHeight = height;
            return this;
        }

        public Builder withVideoBitrate(int bitrate) {
            mBitRate = bitrate;
            return this;
        }

        public RecorderConfig build() {
            return new RecorderConfig(mOutputFile,
                    new VideoEncoderConfig(mWidth, mHeight, mBitRate));
        }


    }
}
