package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;

/**
 * Created by davidbrodsky on 2/17/14.
 */
public class HlsStream extends Stream {

    @Key("bucket_name")
    private String mBucket;

    @Key("aws_region")
    private String mRegion;

    @Key("aws_prefix")
    private String mPrefix;

    @Key("aws_access_key")
    private String mAwsKey;

    @Key("aws_secret_key")
    private String mAwsSecret;

    @Key("aws_session_token")
    private String mToken;

    @Key("aws_duration")
    private String mDuration;

    public String getAwsS3Bucket() {
        return mBucket;
    }

    public String getRegion() {
        return mRegion;
    }

    public String getAwsS3Prefix() {
        return mPrefix;
    }

    public String getAwsKey() {
        return mAwsKey;
    }

    public String getAwsSecret() {
        return mAwsSecret;
    }

    public String getToken() {
        return mToken;
    }

    public String getDuration() {
        return mDuration;
    }

    public String toString(){
        return "Bucket: " + getAwsS3Bucket() + " streamUrl " + getStreamUrl();
    }

}
