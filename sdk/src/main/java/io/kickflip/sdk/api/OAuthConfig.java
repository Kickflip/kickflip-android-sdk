package io.kickflip.sdk.api;

/**
 * Immutable OAuth configuration state
 * @hide
 */
public class OAuthConfig {
    private String credentialStoreName;
    private String userId;
    private String accessTokenRequestUrl;
    private String clientId;
    private String clientSecret;
    private String accessTokenAuthorizeUrl;
    private String callbackUrl;

    public String getCredentialStoreName() {
        return credentialStoreName;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccessTokenRequestUrl() {
        return accessTokenRequestUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAccessTokenAuthorizeUrl() {
        return accessTokenAuthorizeUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public OAuthConfig setCredentialStoreName(String value) {
        if (credentialStoreName == null) credentialStoreName = value;
        return this;
    }

    public OAuthConfig setUserId(String value) {
        if (userId == null) userId = value;
        return this;
    }

    public OAuthConfig setAccessTokenRequestUrl(String value) {
        if (accessTokenRequestUrl == null) accessTokenRequestUrl = value;
        return this;
    }

    public OAuthConfig setClientId(String value) {
        if (clientId == null) clientId = value;
        return this;
    }

    public OAuthConfig setClientSecret(String value) {
        if (clientSecret == null) clientSecret = value;
        return this;
    }

    public OAuthConfig setAccessTokenAuthorizeUrl(String value) {
        if (accessTokenAuthorizeUrl == null) accessTokenAuthorizeUrl = value;
        return this;
    }

    public OAuthConfig setCallbackUrl(String value) {
        if (callbackUrl == null) callbackUrl = value;
        return this;
    }
}