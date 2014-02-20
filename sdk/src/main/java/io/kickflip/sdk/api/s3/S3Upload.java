package io.kickflip.sdk.api.s3;

import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

/**
 * Created by davidbrodsky on 2/19/14.
 */
public class S3Upload {

    private File mFile;

    private String mKey;

    private S3Client mS3;

    public S3Upload(S3Client s3, File file, String key) {
        mFile = file;
        mKey = key;
        mS3 = s3;
    }

    public void upload(){
        mS3.upload(mKey, mFile);
    }
}
