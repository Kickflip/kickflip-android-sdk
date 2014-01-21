package io.kickflip.sdk.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.net.URL;

/**
 * Created by davidbrodsky on 1/16/14.
 */
public abstract class KFEndpointResponse extends GenericJson {

    @Key
    private String type;

    public String getType(){
        return type;
    }

    @Key
    private URL broadcastUrl;

    public URL getBroadcastUrl(){
        return broadcastUrl;
    }

}
