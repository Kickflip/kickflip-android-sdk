package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;

/**
 * JSON API object for Kickflip Responses
 */
public class Response {

    @Key("success")
    private boolean mSuccess;

    @Key("reason")
    private String mReason;

    public Response() {
        // Required default Constructor
    }

    public boolean isSuccessful() {
        return mSuccess;
    }

    public String getReason() {
        return mReason;
    }
}
