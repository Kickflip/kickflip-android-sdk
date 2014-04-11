package io.kickflip.sdk.av;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.Pair;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.kickflip.sdk.FileUtils;
import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.User;
import io.kickflip.sdk.api.s3.S3Client;
import io.kickflip.sdk.api.s3.S3Manager;
import io.kickflip.sdk.api.s3.S3Upload;
import io.kickflip.sdk.events.BroadcastIsBufferingEvent;
import io.kickflip.sdk.events.BroadcastIsLiveEvent;
import io.kickflip.sdk.events.HlsManifestWrittenEvent;
import io.kickflip.sdk.events.HlsSegmentWrittenEvent;
import io.kickflip.sdk.events.MuxerFinishedEvent;
import io.kickflip.sdk.events.S3UploadEvent;
import io.kickflip.sdk.events.StreamLocationAddedEvent;
import io.kickflip.sdk.events.ThumbnailWrittenEvent;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Broadcaster is an AVRecorder that broadcasts its output
 * to Kickflip.
 */
// TODO: Make HLS / RTMP Agnostic
public class Broadcaster extends AVRecorder {
    private static final String TAG = "Broadcaster";
    private static final boolean VERBOSE = true;
    private static final int MIN_BITRATE = 3 * 100 * 1000;              // 300 kbps
    private static ExecutorService mExecutorService;
    private final String VOD_FILENAME = "vod.m3u8";
    private Context mContext;
    private KickflipApiClient mKickflip;
    private User mUser;
    private HlsStream mStream;
    private HlsFileObserver mFileObserver;
    private S3Client mS3Client;
    private ArrayDeque<Pair<String, File>> mUploadQueue;
    private SessionConfig mConfig;
    private BroadcastListener mBroadcastListener;
    private EventBus mEventBus;
    private boolean mReadyToBroadcast;                                  // Kickflip user registered and endpoint ready
    private boolean mSentBroadcastLiveEvent;
    private int mVideoBitrate;
    private File mManifestSnapshotDir;                                  // Directory where manifest snapshots are stored
    private File mVodManifest;                                          // VOD HLS Manifest containing complete history
    private int mNumSegmentsWritten;
    private int mLastRealizedBandwidthBytesPerSec;                      // Bandwidth snapshot for adapting bitrate
    private boolean mDeleteAfterUploading;                              // Should recording files be deleted as they're uploaded?


    public Broadcaster(Context context, SessionConfig config, String API_KEY, String API_SECRET) {
        super(config);
        checkArgument(API_KEY != null && API_SECRET != null);
        init();
        mContext = context;
        mConfig = config;
        mConfig.getMuxer().setEventBus(mEventBus);
        mVideoBitrate = mConfig.getVideoBitrate();
        if (VERBOSE) Log.i(TAG, "Initial video bitrate : " + mVideoBitrate);
        mManifestSnapshotDir = new File(mConfig.getOutputPath().substring(0, mConfig.getOutputPath().lastIndexOf("/") + 1), "m3u8");
        mManifestSnapshotDir.mkdir();
        mVodManifest = new File(mManifestSnapshotDir, VOD_FILENAME);
        writeEventManifestHeader(mConfig.getHlsSegmentDuration());

        String watchDir = config.getOutputPath().substring(0, config.getOutputPath().lastIndexOf(File.separator) + 1);
        mFileObserver = new HlsFileObserver(watchDir, mEventBus);
        mFileObserver.startWatching();

        mReadyToBroadcast = false;
        mKickflip = Kickflip.setup(context, API_KEY, API_SECRET, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                User user = (User) response;
                mUser = user;
                if (VERBOSE) Log.i(TAG, "Got storage credentials " + response);
            }

            @Override
            public void onError(Object response) {
                Log.e(TAG, "Failed to get storage credentials" + response.toString());
                if (mBroadcastListener != null)
                    mBroadcastListener.onBroadcastError();
            }
        });
    }

    private void init() {
        mDeleteAfterUploading = true;
        mLastRealizedBandwidthBytesPerSec = 0;
        mNumSegmentsWritten = 0;
        mSentBroadcastLiveEvent = false;
        mEventBus = new EventBus("Broadcaster");
        mEventBus.register(this);
    }

    private ExecutorService getExecutorService() {
        if (mExecutorService == null)
            mExecutorService = Executors.newSingleThreadExecutor();
        return mExecutorService;
    }

    /**
     * Set whether local recording files be deleted after successful upload. Default is true.
     * <p/>
     * Must be called before recording begins. Otherwise this method has no effect.
     *
     * @param doDelete whether local recording files be deleted after successful upload.
     */
    public void setDeleteLocalFilesAfterUpload(boolean doDelete) {
        if (!isRecording()) {
            mDeleteAfterUploading = doDelete;
        }
    }

    public void setBroadcastListener(BroadcastListener listener) {
        mBroadcastListener = listener;
    }

    public EventBus getEventBus() {
        return mEventBus;
    }

    @Override
    public void startRecording() {
        super.startRecording();
        mCamEncoder.requestThumbnailOnFrameWithScaling(60, 2);
        mKickflip.startStream(mConfig.getStream(), new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                checkArgument(response instanceof HlsStream, "Got unexpected StartStream Response");
                onGotStreamResponse((HlsStream) response);
            }

            @Override
            public void onError(Object response) {
                Log.w(TAG, "Error getting start stream response! " + response);
            }
        });
    }

    private void onGotStreamResponse(HlsStream stream) {
        mStream = stream;
        if (mConfig.shouldAttachLocation()) {
            Kickflip.addLocationToStream(mContext, mStream, mEventBus);
        }
        mStream.setTitle(mConfig.getTitle());
        mStream.setDescription(mConfig.getDescription());
        mStream.setExtraInfo(mConfig.getExtraInfo());
        mStream.setIsPrivate(mConfig.isPrivate());
        if (VERBOSE) Log.i(TAG, "Got hls start stream " + stream);
        mS3Client = new S3Client(S3Client.getBasicAWSCredentials(mStream.getAwsKey(), mStream.getAwsSecret()), mEventBus);
        mS3Client.setBucket(mStream.getBucket());
        mReadyToBroadcast = true;
        submitQueuedUploadsToS3();
        mEventBus.post(new BroadcastIsBufferingEvent());
        if (mBroadcastListener != null) {
            mBroadcastListener.onBroadcastStart();
        }
    }

    public boolean isLive() {
        return mSentBroadcastLiveEvent;
    }

    @Override
    public void stopRecording() {
        super.stopRecording();
        mSentBroadcastLiveEvent = false;
        if (mStream != null) {
            mKickflip.stopStream(mUser, mStream, new KickflipCallback() {
                @Override
                public void onSuccess(Response response) {
                    if (VERBOSE) Log.i(TAG, "Got stop stream response " + response);
                }

                @Override
                public void onError(Object response) {
                    Log.w(TAG, "Error getting stop stream response! " + response);
                }
            });
        }
    }

    /**
     * An S3 Upload completed.
     * <p/>
     * Called on a background thread
     */
    @Subscribe
    public void onS3UploadComplete(S3UploadEvent uploadEvent) {
        if (VERBOSE) Log.i(TAG, "Upload completed for " + uploadEvent.getUrl());
        if (uploadEvent.getUrl().contains(".m3u8")) {
            onManifestUploaded(uploadEvent);
        } else if (uploadEvent.getUrl().contains(".ts")) {
            onSegmentUploaded(uploadEvent);
        } else if (uploadEvent.getUrl().contains(".jpg")) {
            onThumbnailUploaded(uploadEvent);
        }
    }

    /**
     * A .ts file was written in the recording directory.
     * <p/>
     * Use this opportunity to verify the segment is of expected size
     * given the target bitrate
     * <p/>
     * Called on a background thread
     */
    @Subscribe
    public void onSegmentWritten(HlsSegmentWrittenEvent event) {
        try {
            if (Build.VERSION.SDK_INT >= 19 && mConfig.isAdaptiveBitrate()) {
                // Adjust bitrate to match expected filesize
                File hlsSegment = new File(event.getSegmentLocation());
                long actualSegmentSizeBytes = hlsSegment.length();
                long expectedSizeBytes = ((mConfig.getAudioBitrate() / 8) + (mVideoBitrate / 8)) * mConfig.getHlsSegmentDuration();
                float filesizeRatio = actualSegmentSizeBytes / (float) expectedSizeBytes;
                if (VERBOSE)
                    Log.i(TAG, "OnSegmentWritten. Segment size: " + (actualSegmentSizeBytes / 1000) + "kB. ratio: " + filesizeRatio);
                if (filesizeRatio < .7) {
                    if (mLastRealizedBandwidthBytesPerSec != 0) {
                        // Scale bitrate while not exceeding available bandwidth
                        float scaledBitrate = mVideoBitrate * (1 / filesizeRatio);
                        float bandwidthBitrate = mLastRealizedBandwidthBytesPerSec * 8;
                        mVideoBitrate = (int) Math.min(scaledBitrate, bandwidthBitrate);
                    } else {
                        // Scale bitrate to match expected fileSize
                        mVideoBitrate *= (1 / filesizeRatio);
                    }
                    if (VERBOSE) Log.i(TAG, "Scaling video bitrate to " + mVideoBitrate + " bps");
                    adjustVideoBitrate(mVideoBitrate);
                }
                String fileName = hlsSegment.getName();
                queueOrSubmitUpload(keyForFilename(fileName), event.getSegmentLocation());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * An S3 .ts segment upload completed.
     * <p/>
     * Use this opportunity to adjust bitrate based on the bandwidth
     * measured during this segment's transmission.
     * <p/>
     * Called on a background thread
     */
    private void onSegmentUploaded(S3UploadEvent uploadEvent) {
        if (mDeleteAfterUploading) uploadEvent.getFile().delete();
        try {
            if (Build.VERSION.SDK_INT >= 19 && mConfig.isAdaptiveBitrate()) {
                mLastRealizedBandwidthBytesPerSec = uploadEvent.getUploadByteRate();
                // Adjust video encoder bitrate per bandwidth of just-completed upload
                if (VERBOSE) {
                    Log.i(TAG, "Bandwidth: " + (mLastRealizedBandwidthBytesPerSec / 1000.0) + " kBps. Encoder: " + ((mVideoBitrate + mConfig.getAudioBitrate()) / 8) / 1000.0 + " kBps");
                }
                if (mLastRealizedBandwidthBytesPerSec < (((mVideoBitrate + mConfig.getAudioBitrate()) / 8))) {
                    // The new bitrate is equal to the last upload bandwidth, never inferior to MIN_BITRATE, nor superior to the initial specified bitrate
                    mVideoBitrate = Math.max(Math.min(mLastRealizedBandwidthBytesPerSec * 8, mConfig.getVideoBitrate()), MIN_BITRATE);
                    if (VERBOSE) {
                        Log.i(TAG, String.format("Adjusting video bitrate to %f kBps. Bandwidth: %f kBps",
                                mVideoBitrate / (8 * 1000.0), mLastRealizedBandwidthBytesPerSec / 1000.0));
                    }
                    adjustVideoBitrate(mVideoBitrate);
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "OnSegUpload excep");
            e.printStackTrace();
        }
    }

    /**
     * An S3 .m3u8 upload completed.
     * <p/>
     * Called on a background thread
     */
    private void onManifestUploaded(S3UploadEvent uploadEvent) {
        if (mDeleteAfterUploading){
            uploadEvent.getFile().delete();
            String uploadUrl = uploadEvent.getUrl();
            if (uploadUrl.substring(uploadUrl.lastIndexOf(File.separator)+1).equals("vod.m3u8")) {
                mConfig.getOutputDirectory().delete();
            }
        }
        if (!mSentBroadcastLiveEvent) {
            mEventBus.post(new BroadcastIsLiveEvent(((HlsStream) mStream).getKickflipUrl()));
            mSentBroadcastLiveEvent = true;
            if (mBroadcastListener != null)
                mBroadcastListener.onBroadcastLive(((HlsStream) mStream).getKickflipUrl());
        }
    }

    /**
     * A thumbnail upload completed.
     * <p/>
     * Called on a background thread
     */
    private void onThumbnailUploaded(S3UploadEvent uploadEvent) {
        if (mDeleteAfterUploading) uploadEvent.getFile().delete();
        if (mStream != null) {
            mStream.setThumbnailUrl(uploadEvent.getUrl());
            sendStreamMetaData();
        }
    }

    @Subscribe
    public void onStreamLocationAdded(StreamLocationAddedEvent event) {
        sendStreamMetaData();
    }

    @Subscribe
    public void onDeadEvent(DeadEvent e) {
        if (VERBOSE) Log.i(TAG, "DeadEvent ");
    }

    /**
     * A .m3u8 file was written in the recording directory.
     * <p/>
     * Called on a background thread
     */
    @Subscribe
    public void onManifestUpdated(HlsManifestWrittenEvent e) {
        if (VERBOSE) Log.i(TAG, "onManifestUpdated");
        // Copy m3u8 at this moment and queue it to uploading
        // service
        final File orig = new File(e.getManifestLocation());
        final File copy = new File(mManifestSnapshotDir, orig.getName()
                .replace(".m3u8", "_" + mNumSegmentsWritten + ".m3u8"));
        try {
            FileUtils.copy(orig, copy);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        queueOrSubmitUpload(keyForFilename("index.m3u8"), copy.getAbsolutePath());
        appendLastManifestEntryToEventManifest(orig, !isRecording());
        mNumSegmentsWritten++;
    }

    /**
     * A thumbnail was written in the recording directory.
     * <p/>
     * Called on a background thread
     */
    @Subscribe
    public void onThumbnailWritten(ThumbnailWrittenEvent e) {
        queueOrSubmitUpload(keyForFilename("thumb.jpg"), e.getThumbnailLocation());
    }

    @Subscribe
    public void onMuxerFinished(MuxerFinishedEvent e) {
        // TODO: Broadcaster uses AVRecorder reset()
        // this seems better than nulling and recreating Broadcaster
        // since it should be usable as a static object for
        // bg recording
    }

    private void sendStreamMetaData() {
        if (mStream != null) {
            mKickflip.setStreamInfo(mStream, null);
        }
    }

    private String keyForFilename(String fileName) {
        return mUser.getName() + File.separator
                + mStream.getStreamId() + File.separator
                + fileName;
    }

    /**
     * Handle an upload, either submitting to the S3 client
     * or queueing for submission once credentials are ready
     *
     * @param key          destination key
     * @param fileLocation local file
     */
    private void queueOrSubmitUpload(String key, String fileLocation) {
        if (mReadyToBroadcast) {
            S3Manager.queueUpload(new S3Upload(mS3Client, new File(fileLocation), key));
        } else {
            if (VERBOSE) Log.i(TAG, "queueing " + key);
            queueUpload(key, fileLocation);
        }
    }

    /**
     * Queue an upload for later submission to S3
     *
     * @param key          destination key
     * @param fileLocation local file
     */
    private void queueUpload(String key, String fileLocation) {
        if (mUploadQueue == null)
            mUploadQueue = new ArrayDeque<>();
        mUploadQueue.add(new Pair<>(key, new File(fileLocation)));
    }

    /**
     * Submit all queued uploads to the S3 client
     */
    private void submitQueuedUploadsToS3() {
        if (mUploadQueue == null) return;
        for (Pair<String, File> pair : mUploadQueue) {
            S3Manager.queueUpload(new S3Upload(mS3Client, pair.second, pair.first));
        }
    }

    private void writeEventManifestHeader(int targetDuration) {
        FileUtils.writeStringToFile(
                String.format("#EXTM3U\n" +
                        "#EXT-X-PLAYLIST-TYPE:VOD\n" +
                        "#EXT-X-VERSION:3\n" +
                        "#EXT-X-MEDIA-SEQUENCE:0\n" +
                        "#EXT-X-TARGETDURATION:%d\n", targetDuration + 1),
                mVodManifest, false
        );
    }

    private void appendLastManifestEntryToEventManifest(File sourceManifest, boolean lastEntry) {
        String result = FileUtils.tail2(sourceManifest, lastEntry ? 3 : 2);
        FileUtils.writeStringToFile(result, mVodManifest, true);
        if (lastEntry) {
            S3Manager.queueUpload(new S3Upload(mS3Client, mVodManifest, keyForFilename("vod.m3u8")));
            if (VERBOSE) Log.i(TAG, "Queued master manifest " + mVodManifest.getAbsolutePath());
        }
    }

}
