package io.kickflip.sdk.av;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Trace;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by davidbrodsky on 1/23/14.
 */
public class MicrophoneEncoder implements Runnable{
    private static final boolean TRACE = false;
    private static final boolean VERBOSE = false;
    private static final String TAG = "MicrophoneEncoder";

    protected static final int SAMPLES_PER_FRAME = 1024;                            // AAC frame size. Audio encoder input size is a multiple of this
    protected static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord mAudioRecord;
    private AudioEncoderCore mEncoderCore;

    private long mStartTimeNs;
    private boolean mRecordingRequested;

    private SyncEvent mSyncEvent;        // Optional object to wait on before submitting frames

    public MicrophoneEncoder(RecorderConfig config){
        mEncoderCore = new AudioEncoderCore(config.getNumAudioChannels(),
                config.getAudioBitrate(),
                config.getAudioSamplerate(),
                config.getMuxer());
        mRecordingRequested = false;
        setupAudioRecord();
    }


    private void setupAudioRecord(){
        int minBufferSize = AudioRecord.getMinBufferSize(mEncoderCore.mSampleRate,
                mEncoderCore.mChannelConfig, AUDIO_FORMAT);
        int bufferSize = SAMPLES_PER_FRAME * 10;
        if (bufferSize < minBufferSize)
            bufferSize = ((minBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,       // source
                mEncoderCore.mSampleRate,            // sample rate, hz
                mEncoderCore.mChannelConfig,         // channels
                AUDIO_FORMAT,                        // audio format
                bufferSize);                         // buffer size (bytes)

        mAudioRecord.getState();
    }

    public void stopRecording(){
        mRecordingRequested = false;
    }

    public void startRecording(){
        mRecordingRequested = true;
        startAudioRecord();
    }

    public boolean isRecording(){
        return mRecordingRequested;
    }


    private void startAudioRecord(){
        if(mAudioRecord != null){
            Thread audioEncodingThread = new Thread(this, "MicrophoneEncoder");
            audioEncodingThread.setPriority(Thread.MAX_PRIORITY);
            audioEncodingThread.start();
        }
    }

    @Override
    public void run() {
        mAudioRecord.startRecording();
        mStartTimeNs = System.nanoTime();
        Log.i(TAG, "Begin Audio transmission to encoder");
        while(mRecordingRequested){

            if (TRACE) Trace.beginSection("drainAudio");
            mEncoderCore.drainEncoder(false);
            if (TRACE) Trace.endSection();

            if (TRACE) Trace.beginSection("sendAudio");
            sendAudioToEncoder(false);
            if (TRACE) Trace.endSection();

        }

        Log.i(TAG, "Exiting audio encode loop. Draining Audio Encoder");
        if (TRACE) Trace.beginSection("sendAudio");
        sendAudioToEncoder(true);
        if (TRACE) Trace.endSection();
        mAudioRecord.stop();
        mEncoderCore.drainEncoder(true);
        mEncoderCore.release();
    }

    // Variables recycled between calls to sendAudioToEncoder
    MediaCodec mMediaCodec;
    int audioInputBufferIndex;
    int audioInputLength;
    long audioRelativePresentationTimeUs;

    private void sendAudioToEncoder(boolean endOfStream) {
        if(mMediaCodec == null)
            mMediaCodec = mEncoderCore.getMediaCodec();
        // send current frame data to encoder
        try {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            audioInputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (audioInputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[audioInputBufferIndex];
                inputBuffer.clear();
                audioInputLength =  mAudioRecord.read(inputBuffer, SAMPLES_PER_FRAME * 2);
                audioRelativePresentationTimeUs = (System.nanoTime() - mStartTimeNs) / 1000;
                audioRelativePresentationTimeUs -= (audioInputLength / mEncoderCore.mSampleRate ) / 1000000;
                if(audioInputLength == AudioRecord.ERROR_INVALID_OPERATION)
                    Log.e(TAG, "Audio read error");
                //Log.i(TAG, "queueing " + audioInputLength + " audio bytes with pts " + audioRelativePresentationTimeUs);
                if (VERBOSE) Log.i(TAG, "queueing " + audioInputLength + " audio bytes with pts " + audioRelativePresentationTimeUs);
                if (endOfStream) {
                    Log.i(TAG, "EOS received in sendAudioToEncoder");
                    mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, audioInputLength, audioRelativePresentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, audioInputLength, audioRelativePresentationTimeUs, 0);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "_offerAudioEncoder exception");
            t.printStackTrace();
        }
    }

}
