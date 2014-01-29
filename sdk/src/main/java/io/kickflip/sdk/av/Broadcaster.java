package io.kickflip.sdk.av;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayDeque;

import io.kickflip.sdk.KickflipAuthCallback;
import io.kickflip.sdk.HlsFileObserver;
import io.kickflip.sdk.KickflipApiClient;
import io.kickflip.sdk.events.HlsManifestWrittenEvent;
import io.kickflip.sdk.events.HlsSegmentWrittenEvent;
import io.kickflip.sdk.events.UploadedEvent;
import io.kickflip.sdk.json.KickflipAwsResponse;
import io.kickflip.sdk.s3.S3Client;

/**
 * Broadcaster is an AVRecorder that broadcasts its output
 * to Kickflip.
 */
public class Broadcaster extends AVRecorder {
    private static final String TAG = "Broadcaster";

    private KickflipApiClient mKickflip;
    private KickflipAwsResponse mKickflipCredentials;
    private EventBus mEventBus;
    private HlsFileObserver mFileObserver;
    private S3Client mS3Client;
    private ArrayDeque<Pair<String, String>> mUploadQueue;
    private RecorderConfig mConfig;
    private boolean mKickFlipCredentialsReceived;


    public Broadcaster(Context context, RecorderConfig config, String API_KEY, String API_SECRET) {
        super(config);
        mConfig = config;
        mEventBus = new EventBus("BroadcastBus");
        mEventBus.register(this);

        String watchDir = config.getOutputPath().substring(0, config.getOutputPath().lastIndexOf(File.separator)+1);
        mFileObserver = new HlsFileObserver(watchDir, mEventBus);
        mFileObserver.startWatching();

        mKickFlipCredentialsReceived = false;
        mKickflip = new KickflipApiClient(context, API_KEY, API_SECRET, new KickflipAuthCallback() {
            @Override
            public void onSuccess(KickflipAwsResponse response) {
                mKickflipCredentials = response;
                mS3Client = new S3Client(response.asBasicAWSCredentials(), mEventBus);
                mS3Client.setBucket(response.getAppName());
                mKickFlipCredentialsReceived = true;
                Log.i(TAG, "Got storage credentials " + response);
            }

            @Override
            public void onError(Object response) {
                Log.e(TAG, "Failed to get storage credentials" + response.toString());
            }
        });
    }

    @Subscribe
    public void onDeadEvent(DeadEvent e){
        Log.i(TAG, "Dead event!");
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
        return mKickflipCredentials.getName() + File.separator
                + mConfig.getUUID().toString() + File.separator
                + fileName;
    }

    /**
     * Handle an upload, either submitting to the S3 client
     * or queueing for submission once credentials are ready
     * @param key destination key
     * @param fileLocation local file
     */
    private void queueOrSubmitUpload(String key, String fileLocation){
        if(mKickFlipCredentialsReceived){
            mS3Client.upload(key, new File(fileLocation));
        }else{
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
    private void emptyQueue(){
        for(Pair<String, String> pair : mUploadQueue){
            mS3Client.upload(pair.first, new File(pair.second));
        }
    }

}
