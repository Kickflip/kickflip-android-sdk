package io.kickflip.sdk.api.json;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * JSON API object for Kickflip User
 *
 */
public class User extends Response {
    public static final String TAG = "User";

    @SerializedName("app")
    private String mApp;

    @SerializedName("display_name")
    private String mDisplayName;

    @SerializedName("name")
    private String mName;

    @SerializedName("uuid")
    private String mUUID;

    @SerializedName("extra_info")
    private String mExtraInfoStr;

    @SerializedName("avatar_url")
    private String mAvatarUrl;

    public User(String app, String name, String uuid, Map extraInfo) {
        mApp = app;
        mName = name;
        mUUID = uuid;
        if (extraInfo != null)
            mExtraInfoStr = new Gson().toJson(extraInfo);
    }

    public User(){
        // Required Default Constructor
    }

    public String getApp() {
        return mApp;
    }

    public String getName() {
        return mName;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getUUID() {
        return mUUID;
    }

    public Map getExtraInfo() {
        try {
            return (mExtraInfoStr == null || mExtraInfoStr.trim().length() == 0 ? null : new Gson().fromJson(mExtraInfoStr, Map.class));
        } catch (IllegalStateException e) {
            Log.e(TAG, "Unable to deserialize extraInfo");
            return null;
        }
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

}
