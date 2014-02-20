package io.kickflip.sdk.events;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public interface UploadEvent {

//    public UploadEvent(String url, int bytesPerSecond) {
//        this.mUrl = url;
//        this.mBytesPerSecond = bytesPerSecond;
//    }

    public String getUrl();

    /**
     * Returns the rate of the upload
     * @return the rate of the completed upload
     * in bytes per second.
     */
    public int getUploadByteRate();
}
