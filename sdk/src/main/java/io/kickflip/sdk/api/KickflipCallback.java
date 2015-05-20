package io.kickflip.sdk.api;

import io.kickflip.sdk.exception.KickflipException;

/**
 * Generic callback interface for Kickflip
 */
public interface KickflipCallback<T> {
    void onSuccess(T response);
    void onError(KickflipException error);
}
