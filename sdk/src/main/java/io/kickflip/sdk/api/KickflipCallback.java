package io.kickflip.sdk.api;

import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.exception.KickflipException;

/**
 * Callback interface for Kickflip API calls
 *
 */
public interface KickflipCallback {
    public void onSuccess(Response response);
    public void onError(KickflipException error);
}
