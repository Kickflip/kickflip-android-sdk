package io.kickflip.sdk.event;

import java.io.File;

/**
 * Created by davidbrodsky on 2/19/14.
 */
public class S3UploadEvent extends BroadcastEvent implements UploadEvent {
    private static final String TAG = "S3UploadEvent";

    private File mFile;
    private String mUrl;
    private int mBytesPerSecond;

    public File getFile() {
        return mFile;
    }

    @Override
    public String getDestinationUrl() {
        return mUrl;
    }

    @Override
    public int getUploadByteRate() {
        return mBytesPerSecond;
    }

    public S3UploadEvent(File file, String url, int bytesPerSecond) {
        mFile = file;
        mUrl = url;
        mBytesPerSecond = bytesPerSecond;
    }

    public String toString(){
        return "upload to " + mUrl + " at " + mBytesPerSecond + " bps";
    }
}
