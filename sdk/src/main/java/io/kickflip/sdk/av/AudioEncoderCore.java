package io.kickflip.sdk.av;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;

/**
 * @hide
 */
public class AudioEncoderCore extends AndroidEncoder {

    private static final String TAG = "AudioEncoderCore";
    private static final boolean VERBOSE = false;

    protected static final String MIME_TYPE = "audio/mp4a-latm";                    // AAC Low Overhead Audio Transport Multiplex

    // Configurable options
    protected int mChannelConfig;
    protected int mSampleRate;

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    public AudioEncoderCore(int numChannels, int bitRate, int sampleRate, Muxer muxer) throws IOException {
        switch (numChannels) {
            case 1:
                mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
                break;
            case 2:
                mChannelConfig = AudioFormat.CHANNEL_IN_STEREO;
                break;
            default:
                throw new IllegalArgumentException("Invalid channel count. Must be 1 or 2");
        }
        mSampleRate = sampleRate;
        mMuxer = muxer;
        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE, mSampleRate, mChannelConfig);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, numChannels);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();

        mTrackIndex = -1;
    }

    /**
     * Depending on this method ties AudioEncoderCore
     * to a MediaCodec-based implementation.
     * <p/>
     * However, when reading AudioRecord samples directly
     * to MediaCode's input ByteBuffer we can avoid a memory copy
     * TODO: Measure performance gain and remove if negligible
     * @return
     */
    public MediaCodec getMediaCodec(){
        return mEncoder;
    }

    @Override
    protected boolean isSurfaceInputEncoder() {
        return false;
    }

}
