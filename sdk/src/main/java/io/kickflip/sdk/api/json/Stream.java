package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;

/**
 * Created by davidbrodsky on 2/17/14.
 */
public class Stream extends Response {

    @Key("streamID")
    private String mStreamId;

    @Key("stream_type")
    private String mStreamType;

    public String getStreamId() {
        return mStreamId;
    }

    public String getStreamType() {
        return mStreamType;
    }
}
