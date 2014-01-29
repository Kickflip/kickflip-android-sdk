package io.kickflip.sdk.events;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public class HlsSegmentWrittenEvent extends BroadcastEvent {

    private String mSegmentLocation;

    public HlsSegmentWrittenEvent(String segmentLocation) {
        this.mSegmentLocation = segmentLocation;
    }

    public String getSegmentLocation() {
        return mSegmentLocation;
    }

}
