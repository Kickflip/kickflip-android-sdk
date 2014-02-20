package io.kickflip.sdk.events;

/**
 * Created by davidbrodsky on 2/19/14.
 */
public class BroadcastIsLiveEvent extends BroadcastEvent implements UploadEvent {

    private String mUrl;

    private int mBytesPerSecond;

    public BroadcastIsLiveEvent(String url, int bytesPerSecond) {
        super();
        mUrl = url;
        mBytesPerSecond = bytesPerSecond;
    }

    @Override
    public String getUrl() {
        return mUrl;
    }

    @Override
    public int getUploadByteRate() {
        return mBytesPerSecond;
    }
}
