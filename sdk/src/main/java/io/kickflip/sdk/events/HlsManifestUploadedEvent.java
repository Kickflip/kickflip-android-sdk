package io.kickflip.sdk.events;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public class HlsManifestUploadedEvent extends BroadcastEvent {

    private String mUrl;

    public HlsManifestUploadedEvent(String manifestUrl) {
        this.mUrl = manifestUrl;
    }

    public String getUrl() {
        return mUrl;
    }

}
