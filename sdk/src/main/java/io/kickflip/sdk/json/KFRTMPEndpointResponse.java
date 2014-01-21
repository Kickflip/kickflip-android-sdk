package io.kickflip.sdk.json;

import com.google.api.client.util.Key;

import java.net.URL;

/**
 * Created by davidbrodsky on 1/16/14.
 */
public class KFRTMPEndpointResponse extends KFEndpointResponse {

    @Key
    private URL rtmpUrl;

}
