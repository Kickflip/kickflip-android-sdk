package io.kickflip.sdk.api;

import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.exception.KickflipException;

/**
 * Callback interface for Kickflip API calls
 *
 */
public interface KickflipCallback {
    void onSuccess(Response response);
    void onError(KickflipException error);
}
