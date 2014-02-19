package io.kickflip.sdk.api;

import io.kickflip.sdk.api.json.Response;

/**
 * Callback interface for Kickflip API calls
 *
 */
public interface KickflipCallback {
    public void onSuccess(Response response);
    public void onError(Object response);
}
