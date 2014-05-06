package io.kickflip.sdk.av;

/**
 * @hide
 */
public class VideoEncoderConfig {
    protected final int mWidth;
    protected final int mHeight;
    protected final int mBitRate;

    public VideoEncoderConfig(int width, int height, int bitRate) {
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getBitRate() {
        return mBitRate;
    }

    @Override
    public String toString() {
        return "VideoEncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate + " bps";
    }
}