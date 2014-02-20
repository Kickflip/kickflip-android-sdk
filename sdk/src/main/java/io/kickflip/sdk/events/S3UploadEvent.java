package io.kickflip.sdk.events;

import android.util.Log;

/**
 * Created by davidbrodsky on 2/19/14.
 */
public class S3UploadEvent extends BroadcastEvent implements UploadEvent {
    private static final String TAG = "S3UploadEvent";

    private String mUrl;

    private int mBytesPerSecond;

    @Override
    public String getUrl() {
        return mUrl;
    }

    @Override
    public int getUploadByteRate() {
        return mBytesPerSecond;
    }

    public S3UploadEvent(String url, int bytesPerSecond) {
        this.mUrl = url;
        this.mBytesPerSecond = bytesPerSecond;
        Log.i(TAG, this.toString());
    }

    public String toString(){
        return "upload to " + mUrl + " at " + mBytesPerSecond + " bps";
    }
}
