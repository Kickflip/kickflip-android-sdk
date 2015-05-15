package io.kickflip.sdk.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import io.kickflip.sdk.R;
import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.api.json.StreamList;
import io.kickflip.sdk.api.json.User;
import io.kickflip.sdk.exception.KickflipException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Kickflip API Client
 * <p/>
 * After construction, requests can be immediately performed.
 * The client will handle acquiring and refreshing OAuth
 * Access tokens as needed.
 * <p/>
 * The client is intended to manage a unique Kickflip user per Android device installation.
 */
// TODO: Standardize Kickflip server error responses to have detail message
public class KickflipApiClient extends OAuthClient {
    private static final String TAG                 = "KickflipApiClient";
    private static final boolean VERBOSE            = false;
    private static final boolean DEV_ENDPOINT       = false;
    private static final String NEW_USER            = "/user/new";
    private static final String GET_USER_PUBLIC     = "/user/info";
    private static final String GET_USER_PRIVATE    = "/user/uuid";
    private static final String EDIT_USER           = "/user/change";
    private static final String START_STREAM        = "/stream/start";
    private static final String STOP_STREAM         = "/stream/stop";
    private static final String SET_META            = "/stream/change";
    private static final String GET_META            = "/stream/info";
    private static final String FLAG_STREAM         = "/stream/flag";
    private static final String SEARCH_KEYWORD      = "/search";
    private static final String SEARCH_USER         = "/search/user";
    private static final String SEARCH_GEO          = "/search/location";
    private static final String API_VERSION         = "/1.2";
    private static final int MAX_EOF_RETRIES        = 1;
    private static int UNKNOWN_ERROR_CODE           = R.integer.generic_error;    // Error code used when none provided from server
    private static String BASE_URL;
    private JsonObjectParser mJsonObjectParser;             // Re-used across requests
    private JsonFactory mJsonFactory;                       // Re-used across requests

    private Handler mCallbackHandler;                       // Ensure callbacks are posted to consistent thread

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
        if (getActiveUser() == null) {
            createNewUser(cb);
        } else {
            postResponseToCallback(cb, getActiveUser());
            if (VERBOSE)
                Log.i(TAG, "Credentials stored " + getAWSCredentials());
        }
    }

    /**
     * Create a new Kickflip User.
     * The User created as a result of this request is cached and managed by this KickflipApiClient
     * throughout the life of the host Android application installation.
     * <p/>
     * The other methods of this client will be performed on behalf of the user created by this request,
     * unless noted otherwise.
     *
     * @param username    The desired username for this Kickflip User. Will be altered if not unique for this Kickflip app.
     * @param password    The password for this Kickflip user.
     * @param email       The email address for this Kickflip user.
     * @param displayName The display name for this Kickflip user.
     * @param extraInfo   Map data to be associated with this Kickflip User.
     * @param cb          This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *                    or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(io.kickflip.sdk.exception.KickflipException)}.
     */
    public void createNewUser(String username, String password, String email, String displayName, Map extraInfo, final KickflipCallback cb) {
        GenericData data = new GenericData();
        if (username != null) {
            data.put("username", username);
        }

        final String finalPassword;
        if (password != null) {
            finalPassword = password;
        } else {
            finalPassword = generateRandomPassword();
        }
        data.put("password", finalPassword);

        if (displayName != null) {
            data.put("display_name", displayName);
        }
        if (email != null) {
            data.put("email", email);
        }
        if (extraInfo != null) {
            data.put("extra_info", new Gson().toJson(extraInfo));
        }

        post(NEW_USER, new UrlEncodedContent(data), User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "createNewUser response: " + response);
                storeNewUserResponse((User) response, finalPassword);
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "createNewUser Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }

    /**
     * Create a new Kickflip User.
     * The User created as a result of this request is active for this KickflipApiClient instance
     * throughout the life of the host Android application installation, until a subsequent call to this method
     * or {@link #loginUser(String, String, KickflipCallback)} }
     * <p/>
     * The other methods of this client will be performed on behalf of the user created by this request,
     * unless noted otherwise.
     *
     * @param cb This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *           or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(io.kickflip.sdk.exception.KickflipException)}.
     */
    public void createNewUser(final KickflipCallback cb) {
        final String password = generateRandomPassword();
        post(NEW_USER, User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "createNewUser response: " + response);
                storeNewUserResponse((User) response, password);
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "createNewUser Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }

    /**
     * Login an exiting Kickflip User and make it active.
     *
     * @param username The Kickflip user's username
     * @param password The Kickflip user's password
     * @param cb       This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *                 or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(io.kickflip.sdk.exception.KickflipException)}.
     */
    public void loginUser(String username, final String password, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("username", username);
        data.put("password", password);

        post(GET_USER_PRIVATE, new UrlEncodedContent(data), User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "loginUser response: " + response);
                storeNewUserResponse((User) response, password);
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "loginUser Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }

    /**
     * Set the current active user's meta info. Pass a null argument to leave it as-is.
     *
     * @param newPassword the user's new password
     * @param email       the user's new email address
     * @param displayName The desired display name
     * @param extraInfo   Arbitrary String data to associate with this user.
     * @param cb          This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *                    or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(io.kickflip.sdk.exception.KickflipException)}.
     */
    public void setUserInfo(final String newPassword, String email, String displayName, Map extraInfo, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        final String finalPassword;
        if (newPassword != null){
            data.put("new_password", newPassword);
            finalPassword = newPassword;
        } else {
            finalPassword = getPasswordForActiveUser();
        }
        if (email != null) data.put("email", email);
        if (displayName != null) data.put("display_name", displayName);
        if (extraInfo != null) data.put("extra_info", new Gson().toJson(extraInfo));

        post(EDIT_USER, new UrlEncodedContent(data), User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "setUserInfo response: " + response);
                storeNewUserResponse((User) response, finalPassword);
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "setUserInfo Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }

    /**
     * Get public user info
     *
     * @param username The Kickflip user's username
     * @param cb       This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *                 or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(io.kickflip.sdk.exception.KickflipException)}.
     */
    public void getUserInfo(String username, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        data.put("username", username);

        post(GET_USER_PUBLIC, new UrlEncodedContent(data), User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "getUserInfo response: " + response);
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "getUserInfo Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }


    /**
     * Start a new Stream. Must be called after
     * {@link io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)}
     * Delivers stream endpoint destination data via a {@link io.kickflip.sdk.api.KickflipCallback}.
     *
     * @param cb This callback will receive a Stream subclass in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, RtmpStream, etc.
     */
    public void startStream(Stream stream, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        checkNotNull(stream);
        startStreamWithUser(getActiveUser(), stream, cb);
    }

    /**
     * Start a new Stream owned by the given User. Must be called after
     * {@link io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)}
     * Delivers stream endpoint destination data via a {@link io.kickflip.sdk.api.KickflipCallback}.
     *
     * @param user The Kickflip User on whose behalf this request is performed.
     * @param cb   This callback will receive a Stream subclass in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *             depending on the Kickflip account type. Implementors should
     *             check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    private void startStreamWithUser(User user, Stream stream, final KickflipCallback cb) {
        checkNotNull(user);
        checkNotNull(stream);
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
            data.put("extra_info", new Gson().toJson(stream.getExtraInfo()));
        }
        post(START_STREAM, new UrlEncodedContent(data), HlsStream.class, cb);
    }

    /**
     * Stop a Stream. Must be called after
     * {@link io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)} and
     * {@link io.kickflip.sdk.api.KickflipApiClient#startStream(io.kickflip.sdk.api.json.Stream, KickflipCallback)}
     *
     * @param cb This callback will receive a Stream subclass in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, etc.
     */
    public void stopStream(Stream stream, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        stopStream(getActiveUser(), stream, cb);
    }

    /**
     * Stop a Stream owned by the given Kickflip User.
     *
     * @param cb This callback will receive a Stream subclass in #onSuccess(response)
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, etc.
     */
    private void stopStream(User user, Stream stream, final KickflipCallback cb) {
        checkNotNull(stream);
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
        post(STOP_STREAM, new UrlEncodedContent(data), HlsStream.class, cb);
    }

    /**
     * Send Stream Metadata for a {@link io.kickflip.sdk.api.json.Stream}.
     * The target Stream must be owned by the User created with {@link io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)}
     * from this KickflipApiClient.
     *
     * @param stream the {@link io.kickflip.sdk.api.json.Stream} to get Meta data for
     * @param cb     A callback to receive the updated Stream upon request completion
     */
    public void setStreamInfo(Stream stream, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        data.put("stream_id", stream.getStreamId());
        data.put("uuid", getActiveUser().getUUID());
        if (stream.getTitle() != null) {
            data.put("title", stream.getTitle());
        }
        if (stream.getDescription() != null) {
            data.put("description", stream.getDescription());
        }
        if (stream.getExtraInfo() != null) {
            data.put("extra_info", new Gson().toJson(stream.getExtraInfo()));
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

        post(SET_META, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Get Stream Metadata for a a public {@link io.kickflip.sdk.api.json.Stream}.
     * The target Stream must belong a User of your Kickflip app.
     *
     * @param stream the {@link io.kickflip.sdk.api.json.Stream} to get Meta data for
     * @param cb     A callback to receive the updated Stream upon request completion
     */
    public void getStreamInfo(Stream stream, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("stream_id", stream.getStreamId());

        post(GET_META, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Get Stream Metadata for a a public {@link io.kickflip.sdk.api.json.Stream#mStreamId}.
     * The target Stream must belong a User within your Kickflip app.
     * <p/>
     * This method is useful when digesting a Kickflip.io/<stream_id> url, where only
     * the StreamId String is known.
     *
     * @param streamId the stream Id of the given stream. This is the value that appears
     *                 in urls of form kickflip.io/<stream_id>
     * @param cb       A callback to receive the current {@link io.kickflip.sdk.api.json.Stream} upon request completion
     */
    public void getStreamInfo(String streamId, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("stream_id", streamId);

        post(GET_META, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Flag a {@link io.kickflip.sdk.api.json.Stream}. Used when the active Kickflip User does not own the Stream.
     * <p/>
     * To delete a recording the active Kickflip User owns, use
     * {@link io.kickflip.sdk.api.KickflipApiClient#setStreamInfo(io.kickflip.sdk.api.json.Stream, KickflipCallback)}
     *
     * @param stream The Stream to flag.
     * @param cb     A callback to receive the result of the flagging operation.
     */
    public void flagStream(Stream stream, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        data.put("uuid", getActiveUser().getUUID());
        data.put("stream_id", stream.getStreamId());

        post(FLAG_STREAM, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Get a List of {@link io.kickflip.sdk.api.json.Stream} objects created by the given Kickflip User.
     *
     * @param username the target Kickflip username
     * @param cb       A callback to receive the resulting List of Streams
     */
    public void getStreamsByUsername(String username, int pageNumber, int itemsPerPage, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        addPaginationData(pageNumber, itemsPerPage, data);
        data.put("uuid", getActiveUser().getUUID());
        data.put("username", username);
        post(SEARCH_USER, new UrlEncodedContent(data), StreamList.class, cb);
    }

    /**
     * Get a List of {@link io.kickflip.sdk.api.json.Stream}s containing a keyword.
     * <p/>
     * This method searches all public recordings made by Users of your Kickflip app.
     *
     * @param keyword The String keyword to query
     * @param cb      A callback to receive the resulting List of Streams
     */
    public void getStreamsByKeyword(String keyword, int pageNumber, int itemsPerPage, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        addPaginationData(pageNumber, itemsPerPage, data);
        data.put("uuid", getActiveUser().getUUID());
        if (keyword != null) {
            data.put("keyword", keyword);
        }
        post(SEARCH_KEYWORD, new UrlEncodedContent(data), StreamList.class, cb);
    }

    /**
     * Get a List of {@link io.kickflip.sdk.api.json.Stream}s near a geographic location.
     * <p/>
     * This method searches all public recordings made by Users of your Kickflip app.
     *
     * @param location The target Location
     * @param radius   The target Radius in meters
     * @param cb       A callback to receive the resulting List of Streams
     */
    public void getStreamsByLocation(Location location, int radius, int pageNumber, int itemsPerPage, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        data.put("uuid", getActiveUser().getUUID());
        data.put("lat", location.getLatitude());
        data.put("lon", location.getLongitude());
        if (radius != 0) {
            data.put("radius", radius);
        }
        post(SEARCH_GEO, new UrlEncodedContent(data), StreamList.class, cb);
    }

    /**
     * Do a POST Request, creating a new user if necessary
     *
     * @param endpoint      Kickflip endpoint. e.g /user/new
     * @param responseClass Class of the expected response
     * @param cb            Callback that will receive an instance of responseClass
     */
    private void post(final String endpoint, final Class responseClass, final KickflipCallback cb) {
        post(endpoint, null, responseClass, cb);
    }

    /**
     * Do a POST Request, creating a new user if necessary
     *
     * @param endpoint      Kickflip endpoint. e.g /user/new
     * @param body          POST body
     * @param responseClass Class of the expected response
     * @param cb            Callback that will receive an instance of responseClass
     */
    private void post(final String endpoint, final HttpContent body, final Class responseClass, final KickflipCallback cb) {
        acquireAccessToken(new OAuthCallback() {
            @Override
            public void onSuccess(HttpRequestFactory requestFactory) {
                request(requestFactory, METHOD.POST, makeApiUrl(endpoint), body, responseClass, cb);
            }

            @Override
            public void onFailure(Exception e) {
                postExceptionToCallback(cb, UNKNOWN_ERROR_CODE);
            }
        });
    }

    private void request(HttpRequestFactory requestFactory, final METHOD method, final String url, final HttpContent content, final Class responseClass, final KickflipCallback cb) {
        if (VERBOSE)
            Log.i(TAG, String.format("REQUEST: %S : %s body: %s", method, shortenUrlString(url), (content == null ? "" : new GsonBuilder().setPrettyPrinting().create().toJson(content))));
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
                                postExceptionToCallback(cb, UNKNOWN_ERROR_CODE);
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
                postExceptionToCallback(cb, UNKNOWN_ERROR_CODE);
            } catch (ClassCastException e) {
                // A non-HTTP releated error occured.
                Log.w(TAG, String.format("Unhandled Error: %s. Stack trace follows:", e.getMessage()));
                exception.printStackTrace();
                postExceptionToCallback(cb, UNKNOWN_ERROR_CODE);
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
        postExceptionToCallback(cb, UNKNOWN_ERROR_CODE);
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
            postExceptionToCallback(cb, UNKNOWN_ERROR_CODE);
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
            Log.i(TAG, String.format("RESPONSE: %s body: %s", shortenUrlString(response.getRequest().getUrl().toString()), new GsonBuilder().setPrettyPrinting().create().toJson(kickFlipResponse)));
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
            postExceptionToCallback(cb, UNKNOWN_ERROR_CODE);
        } else if (!kickFlipResponse.isSuccessful()) {
            postExceptionToCallback(cb, UNKNOWN_ERROR_CODE);
        } else {
            postResponseToCallback(cb, kickFlipResponse);
        }
    }

    private void storeNewUserResponse(User response, String password) {
        getStorage().edit()
                .putString("app_name", response.getApp())
                .putString("name", response.getName())
                .putString("password", password)
                .putString("uuid", response.getUUID())
                .putString("uuid", response.getUUID())
                .apply();
    }

    private String getPasswordForActiveUser() {
        return getStorage().getString("password", null);
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
     * Get the current active Kickflip User. If no User has been created, returns null.
     * <p/>
     * This will be the User created on the last call to
     * {@link io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)}
     *
     * @return
     */
    public User getActiveUser() {
        SharedPreferences prefs = getStorage();
        if (prefs.contains("uuid") && prefs.contains("name")) {
            return new User(
                    prefs.getString("app_name", ""),
                    prefs.getString("name", ""),
                    prefs.getString("uuid", ""),
                    null);
        } else {
            return null;
        }
    }

    private String getAWSBucket() {
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

    private void postExceptionToCallback(final KickflipCallback cb, final int resourceCodeId) {
        final int errorCode = getContext().getResources().getInteger(resourceCodeId);
        final String message = getContext().getResources().getStringArray(R.array.error_messages)[errorCode];
        KickflipException error = new KickflipException(message, errorCode);
        postExceptionToCallback(cb, error);
    }

    private void postExceptionToCallback(final KickflipCallback cb, final KickflipException exception) {
        if (cb != null) {
            mCallbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onError(exception);
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

    /**
     * Check if a Stream is owned by the active Kickflip User.
     *
     * @param stream the Stream to test.
     * @return true if the active Kickflip User owns the Stream. false otherwise.
     */
    public boolean activeUserOwnsStream(Stream stream) {
        return getActiveUser().getName().compareTo(stream.getOwnerName()) == 0;
    }

    private boolean assertActiveUserAvailable(KickflipCallback cb) {
        if (getActiveUser() == null) {
            Log.e(TAG, "getStreamsByKeyword called before user acquired. If this request needs to be performed on app start," +
                    "call it from the KickflipCallback provided to setup()");
            if (cb != null) {
                postExceptionToCallback(cb, R.integer.user_not_available);
            }
            return false;
        }
        return true;
    }

    private static enum METHOD {GET, POST}

    static {
        if (DEV_ENDPOINT)
            BASE_URL = "http://funkcity.ngrok.com";
        else
            BASE_URL = "https://www.kickflip.io";
    }

    private String generateRandomPassword() {
        return new BigInteger(130, new SecureRandom()).toString(32);
    }

    private void addPaginationData(int pageNumber, int itemsPerPage, GenericData target) {
        target.put("results_per_page", itemsPerPage);
        target.put("page", pageNumber);
    }

    private String makeApiUrl(String endpoint) {
        return BASE_URL + "/api" + API_VERSION + endpoint;
    }

}
