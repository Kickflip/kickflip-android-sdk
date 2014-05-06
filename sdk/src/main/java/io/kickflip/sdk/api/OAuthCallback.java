package io.kickflip.sdk.api;

import com.google.api.client.http.HttpRequestFactory;

/**
 * @hide
 */
public interface OAuthCallback {
    public void onSuccess(HttpRequestFactory oauthRequestFactory);
    public void onFailure(Exception e);
}
