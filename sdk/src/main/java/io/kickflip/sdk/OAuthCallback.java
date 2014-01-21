package io.kickflip.sdk;

import com.google.api.client.http.HttpRequestFactory;

/**
 * Created by davidbrodsky on 1/14/14.
 */
public interface OAuthCallback {
    public void ready(HttpRequestFactory oauthRequestFactory);
}
