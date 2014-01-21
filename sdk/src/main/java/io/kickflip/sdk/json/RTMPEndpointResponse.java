package io.kickflip.sdk.json;

import com.google.api.client.util.Key;

import java.net.URL;

/**
 * Created by davidbrodsky on 1/16/14.
 */
public class RTMPEndpointResponse extends EndpointResponse {

    @Key
    private URL rtmpUrl;

}
