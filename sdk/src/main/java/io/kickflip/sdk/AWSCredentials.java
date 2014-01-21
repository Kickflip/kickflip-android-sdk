package io.kickflip.sdk;

/**
 * Immutable representation of
 * Amazon AWS Credentials
 * Created by davidbrodsky on 1/15/14.
 */
public class AWSCredentials {
    private String mKey;

    public String getmKey() {
        return mKey;
    }

    private String mSecret;

    public String getmSecret() {
        return mSecret;
    }

    private String mBucket;

    public String getBucket() {
        return mBucket;
    }

    public AWSCredentials(String key, String secret, String bucket) {
        mKey = key;
        mSecret = secret;
        mBucket = bucket;
    }
}
