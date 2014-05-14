package io.kickflip.sdk.api.s3;

import android.util.Log;
import android.util.Pair;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.kickflip.sdk.av.Broadcaster;
import io.kickflip.sdk.event.S3UploadEvent;

/**
 * Manages a sequence of S3 uploads on behalf of
 * a single instance of {@link com.amazonaws.auth.AWSCredentials}.
 */
public class S3BroadcastManager implements Runnable {
    private static final String TAG = "S3Manager";
    private static final boolean VERBOSE = false;

    private LinkedBlockingQueue<Pair<PutObjectRequest, Boolean>> mQueue;
    private TransferManager mTransferManager;
    private Broadcaster mBroadcaster;

    public S3BroadcastManager(Broadcaster broadcaster, AWSCredentials creds) {
        mTransferManager = new TransferManager(creds);
        mBroadcaster = broadcaster;
        mQueue = new LinkedBlockingQueue<>();
        new Thread(this).start();
    }

    public void queueUpload(final String bucket, final String key, final File file, boolean lastUpload) {
        if (VERBOSE) Log.i(TAG, "Queueing upload " + key);

        final PutObjectRequest por = new PutObjectRequest(bucket, key, file);
        por.setGeneralProgressListener(new ProgressListener() {
            final String url = "https://" + bucket + ".s3.amazonaws.com/" + key;
            private long uploadStartTime;

            @Override
            public void progressChanged(com.amazonaws.event.ProgressEvent progressEvent) {
                try {
                    if (progressEvent.getEventCode() == ProgressEvent.STARTED_EVENT_CODE) {
                        uploadStartTime = System.currentTimeMillis();
                    } else if (progressEvent.getEventCode() == com.amazonaws.event.ProgressEvent.COMPLETED_EVENT_CODE) {
                        long uploadDurationMillis = System.currentTimeMillis() - uploadStartTime;
                        int bytesPerSecond = (int) (file.length() / (uploadDurationMillis / 1000.0));
                        if (VERBOSE)
                            Log.i(TAG, "Uploaded " + file.length() / 1000.0 + " KB in " + (uploadDurationMillis) + "ms (" + bytesPerSecond / 1000.0 + " KBps)");
                        mBroadcaster.onS3UploadComplete(new S3UploadEvent(file, url, bytesPerSecond));
                    } else if (progressEvent.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
                        Log.w(TAG, "Upload failed for " + url);
                    }
                } catch (Exception excp) {
                    Log.e(TAG, "ProgressListener error");
                    excp.printStackTrace();
                }
            }
        });
        por.setCannedAcl(CannedAccessControlList.PublicRead);
        mQueue.add(new Pair<>(por, lastUpload));
    }

    @Override
    public void run() {
        try {
            boolean lastUploadComplete = false;
            while (!lastUploadComplete) {
                Pair<PutObjectRequest, Boolean> requestPair = mQueue.poll(mBroadcaster.getSessionConfig().getHlsSegmentDuration() + 1, TimeUnit.SECONDS);
                if(requestPair != null) {
                    final PutObjectRequest request = requestPair.first;
                    Upload upload = mTransferManager.upload(request);
                    upload.waitForCompletion();
                    lastUploadComplete = requestPair.second;
                    if (!lastUploadComplete && VERBOSE)
                        Log.i(TAG, "Upload complete.");
                    else if (VERBOSE)
                        Log.i(TAG, "Last Upload complete.");
                } else {
                    if (VERBOSE) Log.e(TAG, "Reached end of Queue before processing last segment!");
                    lastUploadComplete = true;
                }
            }
            if (VERBOSE) Log.i(TAG, "Shutting down");
        } catch (InterruptedException e) {
            Log.w(TAG, "upload interrupted");
            e.printStackTrace();
        }
    }
}
