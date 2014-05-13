package io.kickflip.sdk.event;

import java.io.File;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public class ThumbnailWrittenEvent extends BroadcastEvent {

    private File mThumbnail;

    public ThumbnailWrittenEvent(String thumbLocation) {
        this.mThumbnail = new File(thumbLocation);
    }

    public File getThumbnailFile() {
        return mThumbnail;
    }

}
