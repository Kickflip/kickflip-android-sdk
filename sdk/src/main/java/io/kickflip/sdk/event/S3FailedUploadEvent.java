package io.kickflip.sdk.event;

import java.io.File;

/**
 * Created by David Brodsky on 5/12/14.
 */
public class S3FailedUploadEvent extends BroadcastEvent implements UploadEvent {
    private static final String TAG = "S3FailedUploadEvent";

    private File mFile;

    public File getFile() {
        return mFile;
    }

    @Override
    public String getDestinationUrl() {
        return "";
    }

    @Override
    public int getUploadByteRate() {
        return 0;
    }

    public S3FailedUploadEvent(File file) {
        mFile = file;
    }

    public String toString() {
        return "Failed upload of " + mFile.getName();
    }
}
