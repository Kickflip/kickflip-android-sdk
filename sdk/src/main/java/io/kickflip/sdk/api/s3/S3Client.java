package io.kickflip.sdk.api.s3;

import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.eventbus.EventBus;

import java.io.File;

import io.kickflip.sdk.events.UploadedEvent;

public class S3Client {
    private static final String TAG = "S3Client";

    private AmazonS3Client mS3;
    private BasicAWSCredentials mCredentials;
    private EventBus mEventBus;
    private String mBucket;


    public S3Client(BasicAWSCredentials creds, EventBus eventBus) {
        mCredentials = creds;
        mEventBus = eventBus;
        mS3 = new AmazonS3Client(creds);
    }

    public void setBucket(String bucket) {
        mBucket = bucket;
    }


    /**
     * Begin an upload to S3. Returns the url to the completed upload.
     *
     * @param key    Path relative to provided S3 bucket.
     * @param source File reference to be uploaded.
     * @return
     */
    public void upload(String key, File source) {
        if (mBucket == null) {
            Log.e(TAG, "Bucket not set! Call setBucket(bucketStr)");
            return;
        }
        final String url = "https://" + mBucket + ".s3.amazonaws.com/" + key;
        Log.i(TAG, "Attempting to send " + key + " to bucket " + mBucket + " from file " + source);
        PutObjectRequest por = new PutObjectRequest(mBucket, key, source);
        por.setGeneralProgressListener(new ProgressListener() {
            @Override
            public void progressChanged(com.amazonaws.event.ProgressEvent progressEvent) {
                if (progressEvent.getEventCode() == com.amazonaws.event.ProgressEvent.COMPLETED_EVENT_CODE) {
                    mEventBus.post(new UploadedEvent(url));
                } else if (progressEvent.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
                    Log.w(TAG, "Upload failed for " + url);
                }
            }
        });
        por.setCannedAcl(CannedAccessControlList.PublicRead);
        mS3.putObject(por);
    }


}
