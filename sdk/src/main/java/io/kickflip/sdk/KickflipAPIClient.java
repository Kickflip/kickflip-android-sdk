package io.kickflip.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.util.Map;

import io.kickflip.sdk.json.KickflipAwsResponse;

/**
 * Kickflip OAuth API Client
 * Configured for "Client Credentials" OAuth Flow
 * <p/>
 * After construction, requests can be immediately performed
 * The client will handle acquiring and refreshing the OAuth
 * Access tokens as needed
 */
 // TODO: Figure out valid response types
public class KickflipApiClient extends OAuthClient {
    private static final String TAG = "KickflipApiClient";
    public static final boolean VERBOSE = true;

    public static final String BASE_URL = "http://funkcity.ngrok.com/";

    public static enum METHOD {GET, POST}

    ;

    public KickflipApiClient(Context appContext, String key, String secret) {
        this(appContext, key, secret, null);
    }

    public KickflipApiClient(Context appContext, String key, String secret, KickflipAuthCallback cb) {
        super(appContext, new OAuthConfig()
                .setCredentialStoreName("KF")
                .setClientId(key)
                .setClientSecret(secret)
                .setAccessTokenRequestUrl(BASE_URL + "o/token/")
                .setAccessTokenAuthorizeUrl(BASE_URL + "o/authorize/"));
        initialize(cb);
    }

    public void initialize(KickflipAuthCallback cb) {
        if (!credentialsAcquired()) {
            createNewUser(cb);
        } else {
            cb.onSuccess(getCachedKickflipAwsResponse());
            if (VERBOSE)
                Log.i(TAG, "Credentials stored " + getAWSCredentials());
        }
    }

    /**
     * Create a new Kickflip user and
     * store video storage credentials
     */
    public void createNewUser(final KickflipAuthCallback cb) {
        post(BASE_URL + "api/new/user", KickflipAwsResponse.class, new KickflipAuthCallback() {
            @Override
            public void onSuccess(KickflipAwsResponse response) {
                if (VERBOSE)
                    Log.i(TAG, "createNewUser response: " + response);
                storeUserDetails(response);
                if (cb != null)
                    cb.onSuccess(response);
            }

            @Override
            public void onError(Object response) {
                Log.w(TAG, "createNewUser Error: " + ((KickflipAwsResponse) response));
                if (cb != null)
                    cb.onError(response);
            }
        });
    }

    /**
     * Do a GET Request
     *
     * @param url           String url to GET
     * @param responseClass Class of the expected response
     * @param cb            Callback that will receive an instance of responseClass
     */
    private void get(final String url, final Class responseClass, final KickflipAuthCallback cb) {
        acquireAccessToken(new OAuthCallback() {
            @Override
            public void ready(HttpRequestFactory requestFactory) {
                request(requestFactory, METHOD.GET, url, null, responseClass, cb);
            }
        });
    }

    /**
     * Do a POST Request
     *
     * @param url           String url to POST
     * @param responseClass Class of the expected response
     * @param cb            Callback that will receive an instance of responseClass
     */
    private void post(final String url, final Class responseClass, final KickflipAuthCallback cb) {
        acquireAccessToken(new OAuthCallback() {
            @Override
            public void ready(HttpRequestFactory requestFactory) {
                request(requestFactory, METHOD.POST, url, null, responseClass, cb);
            }
        });
    }

    private void request(HttpRequestFactory requestFactory, METHOD method, final String url, HttpContent content, final Class responseClass, final KickflipAuthCallback cb) {
        Log.i(TAG, String.format("Attempting %S request to %s", method, url));
        try {
            HttpRequest request = null;
            switch (method) {
                case GET:
                    request = requestFactory.buildGetRequest(
                            new GenericUrl(url)).setParser(new JsonObjectParser(new JacksonFactory()));
                    break;
                case POST:
                    request = requestFactory.buildPostRequest(
                            new GenericUrl(url), content).setParser(new JsonObjectParser(new JacksonFactory()));
                    break;
            }
            handleHttpResponse(request.execute(), responseClass, cb);
        } catch (IOException exception) {
            // First try to handle as HttpResponseException
            try {
                HttpResponseException httpException = (HttpResponseException) exception;
                Log.i(TAG, "HttpException: " + httpException.getStatusCode());
                switch (httpException.getStatusCode()) {
                    case 403:
                        // OAuth Access Token invalid
                        Log.i(TAG, "Error 403: OAuth Token appears invalid. Clearing");
                        clearAccessToken();
                        request(requestFactory, method, url, content, responseClass, cb);
                        break;
                    case 400:
                        // Bad Client Credentials
                        Log.e(TAG, "Error 400: Check your Client key and secret");
                        break;
                    default:
                        httpException.printStackTrace();
                }
            } catch (ClassCastException e) {
                exception.printStackTrace();
            }
        }
    }

    private void handleHttpResponse(HttpResponse response, Class<? extends KickflipAwsResponse> responseClass, KickflipAuthCallback cb) throws IOException {
        if (cb != null) {
            Object parsedResponse = response.parseAs(responseClass);
            if (isSuccessResponse(response))
                cb.onSuccess(responseClass.cast(parsedResponse));
            else
                cb.onError(parsedResponse);
        }
    }

    private void storeUserDetails(KickflipAwsResponse response) {
        getStorage().edit().putString("aws_access_key", response.getAwsAccessKey())
                .putString("aws_secret_key", response.getAwsSecretKey())
                .putString("app_name", response.getAppName())
                .putString("name", response.getName())
                .apply();
    }

    public boolean credentialsAcquired() {
        //TODO: Detect account type: HLS or RTMP
        return awsCredentialsAcquired();
    }

    private boolean awsCredentialsAcquired() {
        return getStorage().contains("aws_secret_key");
    }

    private BasicAWSCredentials getAWSCredentials() {
        return new BasicAWSCredentials(
                getStorage().getString("aws_access_key", ""),
                getStorage().getString("aws_secret_key", ""));

    }

    private KickflipAwsResponse getCachedKickflipAwsResponse(){
        SharedPreferences prefs = getStorage();
        return new KickflipAwsResponse(prefs.getString("app_name",""),
                prefs.getString("name",""),
                prefs.getString("aws_access_key",""),
                prefs.getString("aws_secret_key",""));
    }

    public String getAWSBucket(){
        return getStorage().getString("app_name","");
    }

}
