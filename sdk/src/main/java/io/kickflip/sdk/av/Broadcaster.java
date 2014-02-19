package io.kickflip.sdk.av;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayDeque;

import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.HlsFileObserver;
import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.User;
import io.kickflip.sdk.events.HlsManifestWrittenEvent;
import io.kickflip.sdk.events.HlsSegmentWrittenEvent;
import io.kickflip.sdk.events.UploadedEvent;
import io.kickflip.sdk.api.s3.S3Client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Broadcaster is an AVRecorder that broadcasts its output
 * to Kickflip.
 */
// TODO: Make HLS / RTMP Agnostic
public class Broadcaster extends AVRecorder {
    private static final String TAG = "Broadcaster";

    private KickflipApiClient mKickflip;
    private User mUser;
    private HlsStream mStream;
    private EventBus mEventBus;
    private HlsFileObserver mFileObserver;
    private S3Client mS3Client;
    private ArrayDeque<Pair<String, String>> mUploadQueue;
    private RecorderConfig mConfig;
    private boolean mReadyToBroadcast;                      // Kickflip user registered and endpoint ready


    public Broadcaster(Context context, RecorderConfig config, String API_KEY, String API_SECRET) {
        super(config);
        checkArgument(API_KEY != null && API_SECRET != null);
        mConfig = config;
        mEventBus = new EventBus("BroadcastBus");
        mEventBus.register(this);

        String watchDir = config.getOutputPath().substring(0, config.getOutputPath().lastIndexOf(File.separator)+1);
        mFileObserver = new HlsFileObserver(watchDir, mEventBus);
        mFileObserver.startWatching();

        mReadyToBroadcast = false;
        mKickflip = new KickflipApiClient(context, API_KEY, API_SECRET, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                User user = (User) response;
                mUser = user;
                Log.i(TAG, "Got storage credentials " + response);
            }

            @Override
            public void onError(Object response) {
                Log.e(TAG, "Failed to get storage credentials" + response.toString());
            }
        });
    }

    @Override
    public void startRecording(){
        super.startRecording();
        mKickflip.startStream(new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                checkArgument(response instanceof HlsStream);
                mStream = (HlsStream) response;
                Log.i(TAG, "Got hls start stream response " + response);
                mS3Client = new S3Client(mStream.getBasicAWSCredentials(), mEventBus);
                mS3Client.setBucket(mStream.getBucket());
                mReadyToBroadcast = true;
                submitQueuedUploadsToS3();
            }

            @Override
            public void onError(Object response) {
                Log.w(TAG, "Error getting start stream response! " + response);
            }
        });
    }

    @Override
    public void stopRecording(){
        super.stopRecording();
        if(mStream != null){
            mKickflip.stopStream(mUser, mStream, new KickflipCallback() {
                @Override
                public void onSuccess(Response response) {
                    Log.i(TAG, "Got stop stream response " + response);
                }

                @Override
                public void onError(Object response) {
                    Log.w(TAG, "Error getting stop stream response! " + response);
                }
            });
        }
    }

    @Subscribe
    public void onUploaded(UploadedEvent e){
        Log.i(TAG, "Upload completed for " + e.getUrl());
    }

    @Subscribe
    public void onManifestUpdated(HlsManifestWrittenEvent e){
        Log.i(TAG, "onManifestUpdated");
        queueOrSubmitUpload(keyForFile("hls.m3u8"), e.getManifestLocation());
    }

    @Subscribe
    public void onSegmentWritten(HlsSegmentWrittenEvent e){
        Log.i(TAG, "onSegmentWritten");
        String fileName = e.getSegmentLocation().substring(e.getSegmentLocation().lastIndexOf(File.separator) + 1);
        queueOrSubmitUpload(keyForFile(fileName), e.getSegmentLocation());
    }

    private String keyForFile(String fileName){
        return mUser.getName() + File.separator
                + mStream.getStreamId() + File.separator
                + fileName;
    }

    /**
     * Handle an upload, either submitting to the S3 client
     * or queueing for submission once credentials are ready
     * @param key destination key
     * @param fileLocation local file
     */
    private void queueOrSubmitUpload(String key, String fileLocation){
        if(mReadyToBroadcast){
            Log.i(TAG, "uploading " + key);
            mS3Client.upload(key, new File(fileLocation));
        }else{
            Log.i(TAG, "queueing " + key);
            queueUpload(key, fileLocation);
        }
    }

    /**
     * Queue an upload for later submission to S3
     * @param key destination key
     * @param fileLocation local file
     */
    private void queueUpload(String key, String fileLocation){
        if(mUploadQueue == null)
            mUploadQueue = new ArrayDeque<>();
        mUploadQueue.add(new Pair<>(key, fileLocation));
    }

    /**
     * Submit all queued uploads to the S3 client
     */
    private void submitQueuedUploadsToS3(){
        if(mUploadQueue == null) return;
        for(Pair<String, String> pair : mUploadQueue){
            mS3Client.upload(pair.first, new File(pair.second));
        }
    }

}
