package io.kickflip.sdk.api.json;

import com.google.gson.annotations.SerializedName;

/**
 * JSON API object for Kickflip Responses
 */
public class Response {

    @SerializedName("success")
    private boolean mSuccess;

    @SerializedName("reason")
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
