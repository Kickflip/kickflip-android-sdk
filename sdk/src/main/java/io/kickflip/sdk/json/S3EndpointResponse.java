package io.kickflip.sdk.json;

import com.google.api.client.util.Key;

/**
 * Created by davidbrodsky on 1/16/14.
 */
public class S3EndpointResponse extends EndpointResponse {

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
    private String bucket;

    public String getBucket() {
        return bucket;
    }

    @Key
    private String key;

    public String getKey() {
        return key;
    }
}
