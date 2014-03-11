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

//    {
//        "aws_secret_key": "0TJw99/WtbG3ovY6CzsiYXl8/H2+0BrAxHPVoVge",
//            "aws_access_key": "AKIAIHAIKHSD53WPYKWQ",
//            "success": true,
//            "chat_url": "Not implemented yet",
//            "stream_type": "HLS",
//            "kickflip_url": "https://app.kickflip.io/542c4554-d7b8-4ea6-bdd7-251a8f4df764",
//            "bucket_name": "android-test-c3fec7e0-d40f-495c-a25b-1fae44cd5e5d",
//            "streamID": "542c4554-d7b8-4ea6-bdd7-251a8f4df764",
//            "stream_url": "https://android-test-c3fec7e0-d40f-495c-a25b-1fae44cd5e5d.s3.amazon.com/android-test-c3fec7e0-d40f-495c-a25b-1fae44cd5e5d-n20rj1/542c4554-d7b8-4ea6-bdd7-251a8f4df764/index.m3u8",
//            "upload_url": "https://android-test-c3fec7e0-d40f-495c-a25b-1fae44cd5e5d.s3.amazon.com/android-test-c3fec7e0-d40f-495c-a25b-1fae44cd5e5d-n20rj1/542c4554-d7b8-4ea6-bdd7-251a8f4df764/"
//    }
}
