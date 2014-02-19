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

//    {
//        "aws_secret_key":"5R/B7GiAzq9GkGmR13Tk6HlTz95hQNnuHFd2dP/R",
//            "aws_access_key":"AKIAJREVS52GQEOMGZ3A",
//            "app":"kickflip-ios-123",
//            "name":"kickflip-ios-123-w3pi7o",
//            "uuid":"26ac0eea-14c1-4d29-afd7-5d8576aXc1ea"
//    }

}
