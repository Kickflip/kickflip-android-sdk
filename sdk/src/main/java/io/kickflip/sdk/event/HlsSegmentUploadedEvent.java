package io.kickflip.sdk.event;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public class HlsSegmentUploadedEvent extends BroadcastEvent {

    private String mUrl;

    public HlsSegmentUploadedEvent(String mUrl) {
        this.mUrl = mUrl;
    }

    public String getUrl() {
        return mUrl;
    }

}