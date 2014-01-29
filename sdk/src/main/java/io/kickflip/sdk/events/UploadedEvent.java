package io.kickflip.sdk.events;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public class UploadedEvent extends BroadcastEvent {

    private String mUrl;

    public UploadedEvent(String url) {
        this.mUrl = url;
    }

    public String getUrl() {
        return mUrl;
    }

}
