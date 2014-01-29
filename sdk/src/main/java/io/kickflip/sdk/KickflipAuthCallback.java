package io.kickflip.sdk;

import io.kickflip.sdk.json.KickflipAwsResponse;

/**
 * Created by davidbrodsky on 1/15/14.
 */
public interface KickflipAuthCallback {
    public void onSuccess(KickflipAwsResponse response);
    public void onError(Object response);
}
