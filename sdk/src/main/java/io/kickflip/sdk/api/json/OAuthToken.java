package io.kickflip.sdk.api.json;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Kickflip OAuthToken response
 */
public class OAuthToken implements Serializable {

//    {"access_token": "DUY9YW2L6bDZ9alPg3qaWaDBFdqTUR", "scope": "read write", "token_type": "Bearer", "expires_in": 36000}

    @SerializedName("access_token")
    private String mAccessToken;

    @SerializedName("scope")
    private String mScope;

    @SerializedName("token_type")
    private String mTokenType;

    @SerializedName("expires_in")
    private double mExpiresIn;

    public String getAccessToken() {
        return mAccessToken;
    }

    public String getScope() {
        return mScope;
    }

    public String getTokenType() {
        return mTokenType;
    }

    public double getExpiresIn() {
        return mExpiresIn;
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

}
