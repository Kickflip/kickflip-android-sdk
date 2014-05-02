package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;

/**
 * Created by davidbrodsky on 2/17/14.
 */
public class HlsStream extends Stream {

    @Key("bucket_name")
    private String mBucket;

    @Key("aws_access_key")
    private String mAwsKey;

    @Key("aws_secret_key")
    private String mAwsSecret;

    public String getAwsS3Bucket() {
        return mBucket;
    }

    public String getAwsKey() {
        return mAwsKey;
    }

    public String getAwsSecret() {
        return mAwsSecret;
    }

    public String toString(){
        return "Bucket: " + getAwsS3Bucket() + " streamUrl " + getStreamUrl();
    }

}
