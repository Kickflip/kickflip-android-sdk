package io.kickflip.sdk;

/**
 * Created by davidbrodsky on 1/15/14.
 */
public interface APICallback {
    public void onSuccess(Object response);
    public void onError(Object response);
}
