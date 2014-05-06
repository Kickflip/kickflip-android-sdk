package io.kickflip.sdk.av;

/**
 * @hide
 */
public class AudioEncoderConfig {
    protected final int mNumChannels;
    protected final int mSampleRate;
    protected final int mBitrate;

    public AudioEncoderConfig(int channels, int sampleRate, int bitRate) {
        mNumChannels = channels;
        mBitrate = bitRate;
        mSampleRate = sampleRate;
    }

    public int getNumChannels() {
        return mNumChannels;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getBitrate() {
        return mBitrate;
    }

    @Override
    public String toString() {
        return "AudioEncoderConfig: " + mNumChannels + " channels totaling " + mBitrate + " bps @" + mSampleRate + " Hz";
    }
}