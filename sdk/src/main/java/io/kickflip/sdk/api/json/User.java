package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;

/**
 * JSON API object for Kickflip User
 *
 */
public class User extends Response {

    @Key("app")
    private String mApp;

    @Key("name")
    private String mName;

    @Key("uuid")
    private String mUUID;

    public User(String app, String name, String uuid) {
        mApp = app;
        mName = name;
        mUUID = uuid;
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

    public String getUUID() {
        return mUUID;
    }

}
