package io.kickflip.sdk.events;

/**
 * Created by davidbrodsky on 2/19/14.
 */
public class S3UploadEvent extends BroadcastEvent implements UploadEvent {
    private static final String TAG = "S3UploadEvent";

    private String mUrl;

    private int mBytesPerSecond;
    private long mTotalBytes;

    @Override
    public String getUrl() {
        return mUrl;
    }

    @Override
    public int getUploadByteRate() {
        return mBytesPerSecond;
    }

    public S3UploadEvent(String url, int bytesPerSecond, long totalBytes) {
        this.mUrl = url;
        this.mBytesPerSecond = bytesPerSecond;
        this.mTotalBytes = totalBytes;
    }

    public String toString(){
        return "upload to " + mUrl + " at " + mBytesPerSecond + " bps";
    }
}
