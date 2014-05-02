package io.kickflip.sdk.event;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public interface UploadEvent {

    public String getUrl();

    /**
     * Returns the rate of the upload
     * @return the rate of the completed upload
     * in bytes per second.
     */
    public int getUploadByteRate();
}
