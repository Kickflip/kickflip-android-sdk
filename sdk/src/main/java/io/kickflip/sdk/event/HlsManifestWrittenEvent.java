package io.kickflip.sdk.event;

import java.io.File;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public class HlsManifestWrittenEvent extends BroadcastEvent {

    private File mManifest;

    public HlsManifestWrittenEvent(String manifestLocation) {
        mManifest = new File(manifestLocation);
    }

    public File getManifestFile() {
        return mManifest;
    }

}
