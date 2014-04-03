package io.kickflip.sdk.api.json;

import com.amazonaws.auth.BasicAWSCredentials;
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

    public String getBucket() {
        return mBucket;
    }

    public String getAwsKey() {
        return mAwsKey;
    }

    public String getAwsSecret() {
        return mAwsSecret;
    }


    /**
     * Convenience method that returns Amazon
     * BasicAWSCredentials corresponding to this Stream
     * @return BasicAWSCredentials for this stream
     */
    public BasicAWSCredentials getBasicAWSCredentials(){
        return new BasicAWSCredentials(mAwsKey, mAwsSecret);
    }

    public String toString(){
        return "Bucket: " + getBucket() + " streamUrl " + getStreamUrl();
    }

}
