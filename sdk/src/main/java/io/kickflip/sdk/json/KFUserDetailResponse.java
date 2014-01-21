package io.kickflip.sdk.json;

import com.google.api.client.util.Key;

/**
 * KFAPI Response for a new user
 * e.g /api/new/user
 * Created by davidbrodsky on 1/15/14.
 */
public class KFUserDetailResponse {

    @Key
    private String aws_secret_key;

    public String getAwsSecretKey() {
        return aws_secret_key;
    }

    @Key
    private String aws_access_key;

    public String getAwsAccessKey() {
        return aws_access_key;
    }

    @Key
    private String app;

    public String getAppName() {
        return app;
    }

    @Key
    private String name;

    public String getName() {
        return name;
    }

    public String toString() {
        return "app: " + app + ", name: " + name + " aws_access_key: " + aws_access_key + ", aws_secret_key: " + aws_secret_key;
    }

    /*
    {"aws_secret_key": "XXXXXXXXX",
    "aws_access_key": "XXX",
    "app": "streamer-camera-app",
    "name": "streamer-camera-app-derpingtoner"}
     */
}
