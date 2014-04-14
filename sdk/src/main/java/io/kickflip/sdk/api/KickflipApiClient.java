package io.kickflip.sdk.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.util.json.Jackson;
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

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;

import io.kickflip.sdk.R;
import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.api.json.StreamList;
import io.kickflip.sdk.api.json.User;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Kickflip OAuth API Client
 * <p/>
 * After construction, requests can be immediately performed.
 * The client will handle acquiring and refreshing the OAuth
 * Access tokens as needed.
 * <p/>
 * The client is intended to manage a unique Kickflip user per Android device installation.
 */
// TODO: Standardize Kickflip server error responses to have detail message
public class KickflipApiClient extends OAuthClient {
    public static final boolean VERBOSE = false;
    public static final boolean DEV_ENDPOINT = false;
    public static final String NEW_USER = "/api/user/new";
    public static final String START_STREAM = "/api/stream/start";
    public static final String STOP_STREAM = "/api/stream/stop";
    public static final String SET_META = "/api/stream/change";
    public static final String GET_META = "/api/stream/info";
    public static final String FLAG_STREAM = "/api/stream/flag";
    public static final String SEARCH = "/api/search";
    public static final String SEARCH_USER = "/api/search/user";
    public static final String SEARCH_GEO = "/api/search/location";
    private static final int MAX_EOF_RETRIES = 1;
    private static final String TAG = "KickflipApiClient";
    public static String BASE_URL;
    private JsonObjectParser mJsonObjectParser;         // Re-used across requests
    private JsonFactory mJsonFactory;                   // Re-used across requests

    private Handler mCallbackHandler;                   // Ensure callbacks are posted to consistent thread

    /**
     * Construct a KickflipApiClient. All callbacks from this client will occur
     * on the current calling thread.
     *
     * @param appContext Your Application Context
     * @param key        Your Kickflip Account Key
     * @param secret     Your Kickflip Account Secret
     */
    public KickflipApiClient(Context appContext, String key, String secret) {
        this(appContext, key, secret, null);
    }

    /**
     * Construct a KickflipApiClient. All callbacks from this client will occur
     * on the current calling thread.
     *
     * @param appContext Your Application Context
     * @param key        Your Kickflip Account Key
     * @param secret     Your Kickflip Account Secret
     * @param cb         A callback to be notified when the provided Kickflip credentials are verified
     */
    public KickflipApiClient(Context appContext, String key, String secret, KickflipCallback cb) {
        super(appContext, new OAuthConfig()
                .setCredentialStoreName("KF")
                .setClientId(key)
                .setClientSecret(secret)
                .setAccessTokenRequestUrl(BASE_URL + "/o/token/")
                .setAccessTokenAuthorizeUrl(BASE_URL + "/o/authorize/"));
        mCallbackHandler = new Handler();
        initialize(cb);
    }

    private void initialize(KickflipCallback cb) {
        if (!credentialsAcquired()) {
            createNewUser(cb);
        } else {
            postResponseToCallback(cb, getCachedUser());
            if (VERBOSE)
                Log.i(TAG, "Credentials stored " + getAWSCredentials());
        }
    }

    /**
     * Create a new Kickflip User.
     * The User created as a result of this request is cached and managed by this KickflipApiClient
     * throughout the life of the host Android application install.
     *
     * @param cb This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *           or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(Object)}.
     */
    public void createNewUser(final KickflipCallback cb) {
        post(BASE_URL + NEW_USER, User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "createNewUser response: " + response);
                storeNewUserResponse((User) response);
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final Object response) {
                Log.w(TAG, "createNewUser Error: " + response);
                postExceptionToCallback(cb, new KickflipApiException(getContext().getString(R.string.generic_error)));
            }
        });
    }

    /**
     * Start a new Stream with this application's managed Kickflip User. Must be called after
     * {@link io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)}
     * Delivers stream endpoint destination data via a {@link io.kickflip.sdk.api.KickflipCallback}.
     *
     * @param cb This callback will receive a Stream subclass in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, RtmpStream, etc.
     */
    public void startStream(Stream stream, final KickflipCallback cb) {
        startStreamWithUser(getCachedUser(), stream, cb);
    }

    /**
     * Start a new Stream owned by the managed Kickflip User. Must be called after
     * {@link io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)}
     * Delivers stream endpoint destination data via a {@link io.kickflip.sdk.api.KickflipCallback}.
     *
     * @param user The Kickflip User on whose behalf this request is performed.
     * @param cb   This callback will receive a Stream subclass in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *             depending on the Kickflip account type. Implementors should
     *             check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    public void startStreamWithUser(User user, Stream stream, final KickflipCallback cb) {
        checkNotNull(user);
        // TODO: Be HLS / RTMP Agnostic
        GenericData data = new GenericData();
        data.put("uuid", user.getUUID());
        data.put("private", stream.isPrivate());
        if (stream.getTitle() != null) {
            data.put("title", stream.getTitle());
        }
        if (stream.getDescription() != null) {
            data.put("description", stream.getDescription());
        }
        if (stream.getExtraInfo() != null) {
            data.put("extra_info", stream.getExtraInfo());
        }
        post(BASE_URL + START_STREAM, new UrlEncodedContent(data), HlsStream.class, cb);
    }

    /**
     * Stop a Stream owned by the managed Kickflip User. Must be called after
     * {@link io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)} and
     * {@link io.kickflip.sdk.api.KickflipApiClient#startStream(io.kickflip.sdk.api.json.Stream, KickflipCallback)}
     *
     * @param cb This callback will receive a Stream subclass in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    public void stopStream(Stream stream, final KickflipCallback cb) {
        stopStream(getCachedUser(), stream, cb);
    }

    /**
     * Stop a Stream owned by the given Kickflip User.
     *
     * @param cb This callback will receive a Stream subclass in #onSuccess(response)
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    public void stopStream(User user, Stream stream, final KickflipCallback cb) {
        checkNotNull(stream);
        // TODO: Be HLS / RTMP Agnostic
        // TODO: Add start / stop lat lon to Stream?
        GenericData data = new GenericData();
        data.put("stream_id", stream.getStreamId());
        data.put("uuid", user.getUUID());
        if (stream.getLatitude() != 0) {
            data.put("lat", stream.getLatitude());
        }
        if (stream.getLongitude() != 0) {
            data.put("lon", stream.getLongitude());
        }
        post(BASE_URL + STOP_STREAM, new UrlEncodedContent(data), HlsStream.class, cb);
    }

    /**
     * Send Stream Meta data for a {@link io.kickflip.sdk.api.json.Stream} the managed Kickflip User owns
     *
     * @param stream the {@link io.kickflip.sdk.api.json.Stream} to get Meta data for
     * @param cb     A callback to receive the updated Stream upon request completion
     */
    public void setStreamInfo(Stream stream, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("stream_id", stream.getStreamId());
        // TODO: Allow feeding User as argument
        data.put("uuid", getCachedUser().getUUID());
        if (stream.getTitle() != null) {
            data.put("title", stream.getTitle());
        }
        if (stream.getDescription() != null) {
            data.put("description", stream.getDescription());
        }
        if (stream.getExtraInfo() != null) {
            data.put("extra_info", stream.getExtraInfo());
        }
        if (stream.getLatitude() != 0) {
            data.put("lat", stream.getLatitude());
        }
        if (stream.getLongitude() != 0) {
            data.put("lon", stream.getLongitude());
        }
        if (stream.getCity() != null) {
            data.put("city", stream.getCity());
        }
        if (stream.getState() != null) {
            data.put("state", stream.getState());
        }
        if (stream.getCountry() != null) {
            data.put("country", stream.getCountry());
        }

        if (stream.getThumbnailUrl() != null) {
            data.put("thumbnail_url", stream.getThumbnailUrl());
        }

        data.put("private", stream.isPrivate());
        data.put("deleted", stream.isDeleted());

        post(BASE_URL + SET_META, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Get Stream Meta data for a a public {@link io.kickflip.sdk.api.json.Stream} within the managed Kickflip User's
     * Kickflip app
     *
     * @param stream the {@link io.kickflip.sdk.api.json.Stream} to get Meta data for
     * @param cb     A callback to receive the updated Stream upon request completion
     */
    public void getStreamInfo(Stream stream, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("stream_id", stream.getStreamId());

        post(BASE_URL + GET_META, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Get Stream Meta data for a a public stream within the managed Kickflip User's
     * Kickflip app
     *
     * @param streamId the stream Id of the given stream. This is the value that appears
     *                 in urls of form kickflip.io/<stream_id>
     * @param cb       A callback to receive the current {@link io.kickflip.sdk.api.json.Stream} upon request completion
     */
    public void getStreamInfo(String streamId, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("stream_id", streamId);

        post(BASE_URL + GET_META, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Flag a {@link io.kickflip.sdk.api.json.Stream}. Typically used when the managed Kickflip User does not own the Stream.
     *
     * @param stream The Stream to flag.
     * @param cb     A callback to receive the result of the flagging operation.
     */
    public void flagStream(Stream stream, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("uuid", getCachedUser().getUUID());
        data.put("stream_id", stream.getStreamId());

        post(BASE_URL + FLAG_STREAM, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Get a List of {@link io.kickflip.sdk.api.json.Stream} objects created by a particular Kickflip User
     *
     * @param user the target Kickflip User
     * @param cb A callback to receive the resulting List of Streams
     */
    public void getStreamsByUser(User user, String username, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("uuid", user.getUUID());
        data.put("username", username);
        post(BASE_URL + SEARCH_USER, new UrlEncodedContent(data), StreamList.class, cb);
    }

    /**
     * Get a List of {@link io.kickflip.sdk.api.json.Stream} objects containing a keyword within the
     * managed Kickflip User's App.
     *
     * @param user The Kickflip User on whose behalf this request is performed.
     * @param keyword The String keyword to query
     * @param cb A callback to receive the resulting List of Streams
     */
    public void getStreamsByKeyword(User user, String keyword, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("uuid", user.getUUID());
        if (keyword != null) {
            data.put("keyword", keyword);
        }
        post(BASE_URL + SEARCH, new UrlEncodedContent(data), StreamList.class, cb);
    }

    /**
     * Get a List of {@link io.kickflip.sdk.api.json.Stream} objects near a geographic location
     *
     * @param user The Kickflip User on whose behalf this request is performed.
     * @param lat The target Latitude in decimal degrees
     * @param lon The target Longitude in decimal degrees
     * @param radius The target Radius in meters
     * @param cb A callback to receive the resulting List of Streams
     */
    public void getStreamsByLocation(User user, float lat, float lon, int radius, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("uuid", user.getUUID());
        data.put("lat", lat);
        data.put("lon", lon);
        if (radius != 0) {
            data.put("radius", radius);
        }
        post(BASE_URL + SEARCH_GEO, new UrlEncodedContent(data), StreamList.class, cb);
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
            public void onSuccess(HttpRequestFactory requestFactory) {
                request(requestFactory, METHOD.GET, url, null, responseClass, cb);
            }

            @Override
            public void onFailure(Exception e) {
                postExceptionToCallback(cb, e);
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
            public void onSuccess(HttpRequestFactory requestFactory) {
                request(requestFactory, METHOD.POST, url, body, responseClass, cb);
            }

            @Override
            public void onFailure(Exception e) {
                postExceptionToCallback(cb, e);
            }
        });
    }

    private void request(HttpRequestFactory requestFactory, final METHOD method, final String url, final HttpContent content, final Class responseClass, final KickflipCallback cb) {
        if (VERBOSE)
            Log.i(TAG, String.format("REQUEST: %S : %s body: %s", method, shortenUrlString(url), (content == null ? "" : Jackson.toJsonPrettyString(content))));
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
            executeAndRetryRequest(request, responseClass, cb);
        } catch (final IOException exception) {
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
                        acquireAccessToken(new OAuthCallback() {
                            @Override
                            public void onSuccess(HttpRequestFactory oauthRequestFactory) {
                                request(oauthRequestFactory, method, url, content, responseClass, cb);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                postExceptionToCallback(cb, e);
                            }
                        });
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
                if (VERBOSE)
                    Log.i(TAG, "RESPONSE: " + shortenUrlString(url) + " " + exception.getMessage());
                postExceptionToCallback(cb, exception);
            } catch (ClassCastException e) {
                // A non-HTTP releated error occured.
                Log.w(TAG, String.format("Unhandled Error: %s. Stack trace follows:", e.getMessage()));
                exception.printStackTrace();
                postExceptionToCallback(cb, exception);
            }
        }
    }

    /**
     * Execute a HTTPRequest and retry up to {@link io.kickflip.sdk.api.KickflipApiClient#MAX_EOF_RETRIES} times if an EOFException occurs.
     * This is an attempt to address what appears to be a bug in NetHttpTransport
     * <p/>
     * See <a href="https://code.google.com/p/google-api-java-client/issues/detail?id=869&can=4&colspec=Milestone%20Priority%20Component%20Type%20Summary%20ID%20Status%20Owner">This issue</a>
     *
     * @param request
     * @param responseClass
     * @param cb
     * @throws IOException
     */
    private void executeAndRetryRequest(HttpRequest request, Class responseClass, KickflipCallback cb) throws IOException {
        int numRetries = 0;
        while (numRetries < MAX_EOF_RETRIES + 1) {
            try {
                executeAndHandleHttpRequest(request, responseClass, cb);
                // If executeAndHandleHttpRequest completes without throwing EOFException
                // we're good
                return;
            } catch (EOFException eof) {
                if (VERBOSE) Log.i(TAG, "Got EOFException. Retrying..");
                // An EOFException may be due to a bug in the way Connections are recycled
                // within the NetHttpTransport package. Ignore and retry
            }
            numRetries++;
        }
        postExceptionToCallback(cb, new KickflipApiException(getContext().getString(R.string.generic_error)));
    }

    private void executeAndHandleHttpRequest(HttpRequest request, Class responseClass, KickflipCallback cb) throws IOException {
        handleHttpResponse(request.execute(), responseClass, cb);
    }

    /**
     * Verify HTTP response was successful
     * and pass to handleKickflipResponse.
     * <p/>
     * If we have an HttpResponse at all, it means
     * the status code was < 300, so as far as http inspection
     * goes, this method simply enforces status code of 200
     *
     * @param response
     * @param responseClass
     * @param cb            Must not be null
     * @throws IOException
     */
    private void handleHttpResponse(HttpResponse response, Class<? extends Response> responseClass, KickflipCallback cb) throws IOException {
        //Object parsedResponse = response.parseAs(responseClass);
        if (isSuccessResponse(response)) {
            // Http Success
            handleKickflipResponse(response, responseClass, cb);
            //cb.onSuccess(responseClass.cast(parsedResponse));
        } else {
            // Http Failure
            if (VERBOSE)
                Log.i(TAG, String.format("RESPONSE (F): %s body: %s", shortenUrlString(response.getRequest().getUrl().toString()), response.getContent().toString()));
            postExceptionToCallback(cb, new KickflipApiException(getContext().getString(R.string.generic_error)));
        }
    }

    /**
     * Parse the HttpResponse as the appropriate Response subclass
     *
     * @param response
     * @param responseClass
     * @param cb
     * @throws IOException
     */
    private void handleKickflipResponse(HttpResponse response, Class<? extends Response> responseClass, KickflipCallback cb) throws IOException {
        if (cb == null) return;
        HashMap responseMap = null;
        Response kickFlipResponse = response.parseAs(responseClass);
        if (VERBOSE)
            Log.i(TAG, String.format("RESPONSE: %s body: %s", shortenUrlString(response.getRequest().getUrl().toString()), Jackson.toJsonPrettyString(kickFlipResponse)));
//        if (Stream.class.isAssignableFrom(responseClass)) {
//            if( ((String) responseMap.get("stream_type")).compareTo("HLS") == 0){
//                kickFlipResponse = response.parseAs(HlsStream.class);
//            } else if( ((String) responseMap.get("stream_type")).compareTo("RTMP") == 0){
//                // TODO:
//            }
//        } else if(User.class.isAssignableFrom(responseClass)){
//            kickFlipResponse = response.parseAs(User.class);
//        }
        if (kickFlipResponse == null) {
            postExceptionToCallback(cb, new KickflipApiException(getContext().getString(R.string.generic_error)));
        } else if (!kickFlipResponse.isSuccessful()) {
            String reason = kickFlipResponse.getReason() == null ?
                    getContext().getString(R.string.generic_error) :
                    kickFlipResponse.getReason();
            postExceptionToCallback(cb, new KickflipApiException(reason));
        } else {
            postResponseToCallback(cb, kickFlipResponse);
        }
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
    public User getCachedUser() {
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

    private void postExceptionToCallback(final KickflipCallback cb, final Exception e) {
        if (cb != null) {
            mCallbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onError(e);
                }
            });
        }
    }

    private void postResponseToCallback(final KickflipCallback cb, final Response response) {
        if (cb != null) {
            mCallbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onSuccess(response);
                }
            });
        }
    }

    /**
     * Given a string like https://api.kickflip.io/api/search
     * return /api/search
     *
     * @param url
     * @return
     */
    private String shortenUrlString(String url) {
        return url.substring(BASE_URL.length());
    }

    public boolean userOwnsStream(Stream stream) {
        return getCachedUser().getName().compareTo(stream.getOwnerName()) == 0;
    }

    public static enum METHOD {GET, POST}

    static {
        if (DEV_ENDPOINT)
            BASE_URL = "http://funkcity.ngrok.com";
        else
            BASE_URL = "https://www.kickflip.io";
    }

    public class KickflipApiException extends Exception {
        public KickflipApiException(String detail) {
            super(detail);
        }
    }

}
