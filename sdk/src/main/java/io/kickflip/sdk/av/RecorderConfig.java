package io.kickflip.sdk.av;

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
         * Configure a RecorderConfig quickly with intelligent path interpretation.
         * Valid inputs are "/path/to/name.m3u8", "/path/to/name.mp4", "rtmp://path/to/endpoint"
         *
         * For file-based outputs (.m3u8, .mp4) the file structure is managed
         * by a recording UUID.
         *
         * Given an absolute file-based outputLocation like:
         *
         * /sdcard/test.m3u8
         *
         * the output will be available in:
         *
         * /sdcard/<UUID>/test.m3u8
         * /sdcard/<UUID>/test0.ts
         * /sdcard/<UUID>/test1.ts
         * ...
         *
         * You can query the final outputLocation after building with
         * RecorderConfig.getOutputPath()
         *
         * @param outputLocation desired output location. For file based recording,
         *                       recordings will be stored at <outputLocationParent>/<UUID>/<outputLocationFileName>
         */
        public Builder(String outputLocation){
            setAVDefaults();
            mUUID = UUID.randomUUID();

            if(outputLocation.contains("rtmp://")){
                mMuxer = FFmpegMuxer.create(outputLocation, Muxer.FORMAT.RTMP);
            }else if(outputLocation.contains(".flv") /*|| outputLocation.contains("f4v") */){
                mMuxer = FFmpegMuxer.create(createRecordingPath(outputLocation), Muxer.FORMAT.RTMP);
            }else if(outputLocation.contains(".m3u8")){
                mMuxer = FFmpegMuxer.create(createRecordingPath(outputLocation), Muxer.FORMAT.HLS);
            }else if(outputLocation.contains(".mp4")){
                mMuxer = AndroidMuxer.create(createRecordingPath(outputLocation), Muxer.FORMAT.MPEG4);
                //mMuxer = FFmpegMuxer.create(createRecordingPath(outputLocation), Muxer.FORMAT.MPEG4);
            }else
                throw new RuntimeException("Unexpected muxer output. Expected a .mp4, .m3u8, or rtmp url: " + outputLocation);

        }

        /**
         * Use this builder to manage file hierarchy manually
         * or to provide your own Muxer
         * @param muxer
         */
        public Builder(Muxer muxer) {
            setAVDefaults();
            mMuxer = checkNotNull(muxer);
            mUUID = UUID.randomUUID();
        }

        /**
         * Inserts a directory into the given path based on the
         * value of mUUID.
         *
         * @param outputPath a desired storage location like /path/filename.ext
         * @return a File pointing to /path/UUID/filename.ext
         */
        private String createRecordingPath(String outputPath){
            File desiredFile = new File(outputPath);
            String desiredFilename = desiredFile.getName();
            File outputDir = new File(desiredFile.getParent(), mUUID.toString());
            outputDir.mkdirs();
            return new File(outputDir, desiredFilename).getAbsolutePath();
        }

        private void setAVDefaults(){
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
