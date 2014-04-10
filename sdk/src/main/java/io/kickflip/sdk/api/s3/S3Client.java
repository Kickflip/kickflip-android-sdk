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

import io.kickflip.sdk.events.S3UploadEvent;

public class
        S3Client {
    private static final String TAG = "S3Client";
    private static final boolean VERBOSE = false;

    private AmazonS3Client mS3;
    private BasicAWSCredentials mCredentials;
    private EventBus mEventBus;
    private String mBucket;

    public S3Client(BasicAWSCredentials creds, EventBus eventBus) {
        mCredentials = creds;
        mEventBus = eventBus;
        mS3 = new AmazonS3Client(creds);
    }

    /**
     * Convenience method that returns Amazon
     * BasicAWSCredentials corresponding to this Stream
     * @return BasicAWSCredentials for this stream
     */
    public static BasicAWSCredentials getBasicAWSCredentials(String awsKey, String awsSecret){
        return new BasicAWSCredentials(awsKey, awsSecret);
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
        if (VERBOSE) Log.i(TAG, "Attempting to send " + key + " to bucket " + mBucket + " from file " + source);
        PutObjectRequest por = new PutObjectRequest(mBucket, key, source);
        final long fileLength = source.length();
        final long startTime = System.currentTimeMillis();
        por.setGeneralProgressListener(new ProgressListener() {
            @Override
            public void progressChanged(com.amazonaws.event.ProgressEvent progressEvent) {
            try{
                if (progressEvent.getEventCode() == com.amazonaws.event.ProgressEvent.COMPLETED_EVENT_CODE) {
                    int bytesPerSecond = (int) (fileLength / ((System.currentTimeMillis() - startTime)/1000.0));
                    if (VERBOSE) Log.i(TAG, "Uploaded " + fileLength / 1000.0 + " KB in " + (System.currentTimeMillis() - startTime) + "ms (" + bytesPerSecond / 10000 + " KBps)");
                    mEventBus.post(new S3UploadEvent(url, bytesPerSecond, fileLength));
                } else if (progressEvent.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
                    Log.w(TAG, "Upload failed for " + url);
                }
            } catch (Exception excp){
                Log.e(TAG, "ProgressListener error");
                excp.printStackTrace();
            }
            }
        });
        por.setCannedAcl(CannedAccessControlList.PublicRead);
        mS3.putObject(por);

    }


}
