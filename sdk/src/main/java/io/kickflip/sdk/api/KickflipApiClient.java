package io.kickflip.sdk.api;

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
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.GenericData;

import java.io.IOException;
import java.util.HashMap;

import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.api.json.User;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Kickflip OAuth API Client
 * Configured for "Client Credentials" OAuth Flow
 * <p/>
 * After construction, requests can be immediately performed
 * The client will handle acquiring and refreshing the OAuth
 * Access tokens as needed
 */
public class KickflipApiClient extends OAuthClient {
    private static final String TAG = "KickflipApiClient";
    public static final boolean VERBOSE = true;

    public static final String BASE_URL = "http://funkcity.ngrok.com";
    public static final String NEW_USER = "/api/new/user";
    public static final String START_STREAM = "/api/stream/start";
    public static final String STOP_STREAM = "/api/stream/stop";
    public static enum METHOD {GET, POST}

    private JsonObjectParser mJsonObjectParser;         // Re-used across requests
    private JsonFactory mJsonFactory;                   // Re-used across requests

    public KickflipApiClient(Context appContext, String key, String secret) {
        this(appContext, key, secret, null);
    }

    public KickflipApiClient(Context appContext, String key, String secret, KickflipCallback cb) {
        super(appContext, new OAuthConfig()
                .setCredentialStoreName("KF")
                .setClientId(key)
                .setClientSecret(secret)
                .setAccessTokenRequestUrl(BASE_URL + "/o/token/")
                .setAccessTokenAuthorizeUrl(BASE_URL + "/o/authorize/"));
        initialize(cb);
    }

    public void initialize(KickflipCallback cb) {
        if (!credentialsAcquired()) {
            createNewUser(cb);
        } else {
            cb.onSuccess(getCachedUser());
            if (VERBOSE)
                Log.i(TAG, "Credentials stored " + getAWSCredentials());
        }
    }

    /**
     * Create a new Kickflip user.
     * The response is cached locally.
     *
     * @param cb This callback will receive a User in #onSuccess(response),
     *           or an Error object onError(). Error object type TBD.
     */
    public void createNewUser(final KickflipCallback cb) {
        post(BASE_URL + NEW_USER, User.class, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (VERBOSE)
                    Log.i(TAG, "createNewUser response: " + response);
                storeNewUserResponse((User) response);
                if (cb != null)
                    cb.onSuccess(response);
            }

            @Override
            public void onError(Object response) {
                Log.w(TAG, "createNewUser Error: " + response);
                if (cb != null)
                    cb.onError(response);
            }
        });
    }

    /**
     * Request to start a new Stream.
     * Delivers stream endpoint destination data via cb.
     * Uses the cached User
     *
     * @param cb This callback will receive a Stream subclass in #onSuccess(response)
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream,RtmpStream, etc.
     */
    public void startStream(final KickflipCallback cb) {
        startStreamWithUser(getCachedUser(), cb);
    }

    /**
     * Request to start a new Stream.
     * Delivers stream endpoint destination data via cb.
     *
     * @param cb This callback will receive a Stream subclass in #onSuccess(response)
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    public void startStreamWithUser(User user, final KickflipCallback cb) {
        checkNotNull(user);
        // TODO: Be HLS / RTMP Agnostic
        GenericData data = new GenericData();
        data.put("uuid", user.getUUID());
        post(BASE_URL + START_STREAM,  new UrlEncodedContent(data), HlsStream.class, cb);
    }

    /**
     * Request to stop a Stream.
     *
     * @param cb This callback will receive a Stream subclass in #onSuccess(response)
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    public void stopStream(User user, Stream stream, final KickflipCallback cb) {
        checkNotNull(stream);
        // TODO: Be HLS / RTMP Agnostic
        GenericData data = new GenericData();
        data.put("stream_id", stream.getStreamId());
        data.put("uuid", user.getUUID());
        post(BASE_URL + STOP_STREAM, new UrlEncodedContent(data), HlsStream.class, cb);
    }


    /**
     * Do a GET Request, creating a new user if necessary
     *
     * @param url           String url to GET
     * @param responseClass Class of the expected response
     * @param cb            Callback that will receive an instance of responseClass
     */
    private void get(final String url, final Class responseClass, final KickflipCallback cb) {
        acquireAccessToken(new OAuthCallback() {
            @Override
            public void ready(HttpRequestFactory requestFactory) {
                request(requestFactory, METHOD.GET, url, null, responseClass, cb);
            }
        });
    }

    /**
     * Do a POST Request, creating a new user if necessary
     *
     * @param url           Url
     * @param responseClass Class of the expected response
     * @param cb            Callback that will receive an instance of responseClass
     */
    private void post(final String url, final Class responseClass, final KickflipCallback cb) {
        post(url, null, responseClass, cb);
    }

    /**
     * Do a POST Request, creating a new user if necessary
     *
     * @param url           Url
     * @param body          POST body
     * @param responseClass Class of the expected response
     * @param cb            Callback that will receive an instance of responseClass
     */
    private void post(final String url, final HttpContent body, final Class responseClass, final KickflipCallback cb) {
        acquireAccessToken(new OAuthCallback() {
            @Override
            public void ready(HttpRequestFactory requestFactory) {
                request(requestFactory, METHOD.POST, url, body, responseClass, cb);
            }
        });
    }

    private void request(HttpRequestFactory requestFactory, METHOD method, final String url, HttpContent content, final Class responseClass, final KickflipCallback cb) {
        Log.i(TAG, String.format("Attempting %S : %s body: %s", method, url, (content == null ? "" : content.toString() )));
        try {
            HttpRequest request = null;
            switch (method) {
                case GET:
                    request = requestFactory.buildGetRequest(
                            new GenericUrl(url)).setParser(getJsonObjectParser());
                    break;
                case POST:
                    request = requestFactory.buildPostRequest(
                            new GenericUrl(url), content).setParser(getJsonObjectParser());
                    break;
            }
            handleHttpResponse(request.execute(), responseClass, cb);
        } catch (IOException exception) {
            // First try to handle as HttpResponseException
            try {
                HttpResponseException httpException = (HttpResponseException) exception;
                // If this cast suceeds, the HTTP Status code must be >= 300
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
                        Log.w(TAG, String.format("Unhandled Http Error %d : %s",
                                httpException.getStatusCode(),
                                httpException.getMessage()));
                }
            } catch (ClassCastException e) {
                // A non-HTTP releated error occured.
                Log.w(TAG, String.format("Unhandled Error: %s. Stack trace follows:", e.getMessage()));
                exception.printStackTrace();
            }
        }
    }

    /**
     * Verify HTTP response was successful
     * and pass to handleKickflipResponse.
     *
     * If we have an HttpResponse at all, it means
     * the status code was < 300, so as far as http inspection
     * goes, this method simply enforces status code of 200
     *
     * @param response
     * @param responseClass
     * @param cb Must not be null
     * @throws IOException
     */
    private void handleHttpResponse(HttpResponse response, Class<? extends Response> responseClass, KickflipCallback cb) throws IOException {
        checkNotNull(cb);
        //Object parsedResponse = response.parseAs(responseClass);
        if (isSuccessResponse(response)) {
            // Http Success
            handleKickflipResponse(response, responseClass, cb);
            //cb.onSuccess(responseClass.cast(parsedResponse));
        } else {
            // Http Failure
            cb.onError(response);
        }
    }

    /**
     * Parse the HttpResponse as the appropriate Response subclass
     * @param response
     * @param responseClass
     * @param cb
     * @throws IOException
     */
    private void handleKickflipResponse(HttpResponse response, Class<? extends Response> responseClass, KickflipCallback cb) throws IOException {
        HashMap responseMap = null;
        Response kickFlipResponse = response.parseAs(responseClass);
//        if (Stream.class.isAssignableFrom(responseClass)) {
//            if( ((String) responseMap.get("stream_type")).compareTo("HLS") == 0){
//                kickFlipResponse = response.parseAs(HlsStream.class);
//            } else if( ((String) responseMap.get("stream_type")).compareTo("RTMP") == 0){
//                // TODO:
//            }
//        } else if(User.class.isAssignableFrom(responseClass)){
//            kickFlipResponse = response.parseAs(User.class);
//        }

        if(kickFlipResponse == null || !kickFlipResponse.isSuccessful()){
            cb.onError(response);
        } else
            cb.onSuccess(kickFlipResponse);
    }

    private void storeNewUserResponse(User response) {
        getStorage().edit()
                .putString("app_name", response.getApp())
                .putString("name", response.getName())
                .putString("uuid", response.getUUID())
                .putString("uuid", response.getUUID())
                .apply();
    }

    public boolean credentialsAcquired() {
        //TODO: Detect account type: HLS or RTMP
        return isUserCached();
    }

    private boolean isUserCached() {
        //TODO: Ensure this use belongs to the current app
        return getStorage().contains("uuid");
    }

    private BasicAWSCredentials getAWSCredentials() {
        return new BasicAWSCredentials(
                getStorage().getString("aws_access_key", ""),
                getStorage().getString("aws_secret_key", ""));

    }

    /**
     * Craft a User with data cached in SharedPrefs
     *
     * @return
     */
    private User getCachedUser() {
        SharedPreferences prefs = getStorage();
        return new User(
                prefs.getString("app_name", ""),
                prefs.getString("name", ""),
                prefs.getString("uuid", ""));
    }

    public String getAWSBucket() {
        return getStorage().getString("app_name", "");
    }

    private JsonFactory getJsonFactory() {
        if (mJsonFactory == null)
            mJsonFactory = new JacksonFactory();
        return mJsonFactory;
    }

    private JsonObjectParser getJsonObjectParser() {
        if (mJsonObjectParser == null)
            mJsonObjectParser = new JsonObjectParser(getJsonFactory());
        return mJsonObjectParser;
    }

}
