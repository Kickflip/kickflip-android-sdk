package io.kickflip.sdk.events;

import io.kickflip.sdk.api.json.Stream;

/**
 * Created by davidbrodsky on 2/19/14.
 */
public class BroadcastIsLiveEvent extends BroadcastEvent {

    private String mWatchUrl;

    public BroadcastIsLiveEvent(String watchurl) {
        super();
        mWatchUrl = watchurl;
    }

    public String getWatchUrl(){
        return mWatchUrl;
    }

}
