package io.kickflip.sdk.api.json;

import com.google.gson.annotations.SerializedName;

/**
 * Created by davidbrodsky on 2/17/14.
 */
public class HlsStream extends Stream {

    @SerializedName("bucket_name")
    private String mBucket;

    @SerializedName("aws_region")
    private String mRegion;

    @SerializedName("aws_prefix")
    private String mPrefix;

    @SerializedName("aws_access_key")
    private String mAwsKey;

    @SerializedName("aws_secret_key")
    private String mAwsSecret;

    @SerializedName("aws_session_token")
    private String mToken;

    @SerializedName("aws_duration")
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
