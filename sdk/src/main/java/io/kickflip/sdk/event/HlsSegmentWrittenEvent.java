package io.kickflip.sdk.event;

import java.io.File;

/**
 * Created by davidbrodsky on 1/28/14.
 */
public class HlsSegmentWrittenEvent extends BroadcastEvent {

    private File mSegment;

    public HlsSegmentWrittenEvent(String segmentLocation) {
        mSegment = new File(segmentLocation);
    }

    public File getSegment() {
        return mSegment;
    }

}
