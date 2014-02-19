package io.kickflip.sdk.api.s3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Static singleton that queues uploads
 *
 * This design allows a queue of uploads
 * to span multiple sets of AWSCredentials
 */
public class S3Manager {

    private static ExecutorService mExecutorService;

    private static ExecutorService getExecutorService(){
        if(mExecutorService == null)
            mExecutorService = Executors.newSingleThreadExecutor();
        return mExecutorService;
    }

    public static void queueUpload(S3Upload upload){
        getExecutorService().submit(new UploadTask(upload));
    }

    public static class UploadTask implements Runnable {

        private S3Upload mUpload;

        public UploadTask(S3Upload upload){
            mUpload = upload;
        }

        @Override
        public void run() {
            mUpload.upload();
        }
    }
}
