package io.kickflip.sdk.av;

/**
 * @hide
 */
public class AudioEncoderConfig {
    protected final int mNumChannels;
    protected final int mSampleRate;
    protected final int mBitrate;
    protected final boolean mRecordAudio;

    public AudioEncoderConfig(int channels, int sampleRate, int bitRate) {
        this(channels, sampleRate, bitRate, true);
    }

    public AudioEncoderConfig(int channels, int sampleRate, int bitRate, boolean recordAudio) {
        mNumChannels = channels;
        mBitrate = bitRate;
        mSampleRate = sampleRate;
        mRecordAudio = recordAudio;
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
        if(mRecordAudio)
            return "AudioEncoderConfig: " + mNumChannels + " channels totaling " + mBitrate + " bps @" + mSampleRate + " Hz";
        else
            return "AudioEncoderConfig: Don't record audio";
    }
}