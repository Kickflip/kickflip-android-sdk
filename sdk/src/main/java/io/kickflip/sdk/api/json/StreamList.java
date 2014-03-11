package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * Created by davidbrodsky on 3/11/14.
 */
public class StreamList extends Response {

    @Key("streams")
    private List<Stream> mStreams;

    public List<Stream> getStreams() {
        return mStreams;
    }
}
