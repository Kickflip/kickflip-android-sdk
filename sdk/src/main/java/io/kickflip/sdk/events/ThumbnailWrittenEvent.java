package io.kickflip.sdk.events;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public class ThumbnailWrittenEvent extends BroadcastEvent {

    private String mThumbnailLocation;

    public ThumbnailWrittenEvent(String thumbLocation) {
        this.mThumbnailLocation = thumbLocation;
    }

    public String getThumbnailLocation() {
        return mThumbnailLocation;
    }

}
