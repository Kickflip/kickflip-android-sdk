package io.kickflip.sdk.event;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public class HlsManifestWrittenEvent extends BroadcastEvent {

    private String mManifestLocation;

    public HlsManifestWrittenEvent(String manifestLocation) {
        this.mManifestLocation = manifestLocation;
    }

    public String getManifestLocation() {
        return mManifestLocation;
    }

}
