package io.kickflip.sdk.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.gson.Gson;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;

import hugo.weaving.DebugLog;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.api.json.StreamList;
import io.kickflip.sdk.api.json.User;
import io.kickflip.sdk.exception.KickflipException;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

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
public class KickflipApiClient {
    private static final String TAG = "KickflipApiClient2";

    private Context mContext;
    private String mClientId;                               // Allow checking whether Client Creds have changed
    private String mClientSecret;
    private KickflipService mService;
    private Handler mCallbackHandler;                       // Ensure callbacks are posted to consistent thread
    private Gson mGson;                                     // To convert extraInfo map to json string

    // TODO : Add static creator allowing username, display_name etc.

    /**
     * Return an Observable for a {@link KickflipApiClient} after creating a new Kickflip user
     * for this application instance if necessary
     */
    public static Observable<KickflipApiClient> create(@NonNull final Context context,
                                                       @NonNull final String clientId,
                                                       @NonNull final String clientSecret) {
        checkNotNull(context);
        checkNotNull(clientId);
        checkNotNull(clientSecret);

        final Handler callbackHandler = new Handler();

        return KickflipApiClientGenerator.getService(clientId, clientSecret)
                .map(new Func1<KickflipService, KickflipApiClient>() {
                    @Override
                    public KickflipApiClient call(KickflipService kickflipService) {
                        return new KickflipApiClient(context, kickflipService, clientId, clientSecret, callbackHandler);
                    }
                })
                .flatMap(new Func1<KickflipApiClient, Observable<KickflipApiClient>>() {
                    @Override
                    public Observable<KickflipApiClient> call(final KickflipApiClient kickflipApiClient) {
                        return kickflipApiClient.getOrCreateActiveUser()
                                .map(new Func1<User, KickflipApiClient>() {
                                    @Override
                                    public KickflipApiClient call(User user) {
                                        return kickflipApiClient;
                                    }
                                });
                    }
                });
    }

    private KickflipApiClient(@NonNull Context context,
                              @NonNull KickflipService service,
                              @NonNull String clientId,
                              @NonNull String clientSecret,
                              @NonNull Handler callbackHandler) {
        checkNotNull(context);
        checkNotNull(service);

        mService = service;
        mContext = context;
        mClientId = clientId;
        mClientSecret = clientSecret;
        mCallbackHandler = callbackHandler;

        mGson = new Gson();

    }

//    /**
//     * Construct a KickflipApiClient. All callbacks from this client will occur
//     * on the current calling thread.
//     *
//     * @param appContext Your Application Context
//     * @param key        Your Kickflip Account Key
//     * @param secret     Your Kickflip Account Secret
//     */
//    private KickflipApiClient2(Context appContext, String key, String secret) {
//        this(appContext, key, secret, null);
//    }
//
//    /**
//     * Construct a KickflipApiClient. All callbacks from this client will occur
//     * on the current calling thread.
//     *
//     * @param appContext Your Application Context
//     * @param key        Your Kickflip Account Key
//     * @param secret     Your Kickflip Account Secret
//     * @param cb         A callback to be notified when the provided Kickflip credentials are verified
//     */
//    private KickflipApiClient2(Context appContext, String key, String secret, KickflipCallback cb) {
//        super(appContext, new OAuthConfig()
//                .setCredentialStoreName("KF")
//                .setClientId(key)
//                .setClientSecret(secret)
//                .setAccessTokenRequestUrl(BASE_URL + "/o/token/")
//                .setAccessTokenAuthorizeUrl(BASE_URL + "/o/authorize/"));
//        mCallbackHandler = new Handler();
//        initialize(cb);
//    }

    /**
     * @return the currently active Kickflip User, creating if necessary
     */
    @DebugLog
    private Observable<User> getOrCreateActiveUser() {

        User persistedUser = getActiveUser();
        if (persistedUser != null) {
            Log.d(TAG, "Using persisted user");
            return Observable.just(persistedUser);
        }

        // Create a new user
        Log.d(TAG, "Creating new user");
        final String password = generateRandomPassword();
        return mService.createNewUser(password)
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "Failed to create new user!", throwable);
                    }
                })
                .doOnNext(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        Log.d(TAG, "Persisting user " + user.getDisplayName());
                        storeUser(user, password);
                    }
                });
    }

    public String getClientId() {
        return mClientId;
    }

    public String getClientSecret() {
        return mClientSecret;
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
     * @param cb          This callback will receive a User in {@link KickflipCallback#onSuccess(Object)}
     *                    or an Exception {@link KickflipCallback#onError(KickflipException)}.
     */
    @DebugLog
    public void createNewUser(@Nullable String username,
                              @Nullable String password,
                              @Nullable String email,
                              @Nullable String displayName,
                              @Nullable Map extraInfo,
                              @Nullable final KickflipCallback cb) {

        mService.createNewUser(username, password, displayName, email, mGson.toJson(extraInfo))
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "Failed to create user ", throwable);
                    }
                })
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        if (cb != null) cb.onSuccess(user);
                    }
                });
    }

    @DebugLog
    public Observable<User> createNewUser(@Nullable String username,
                                          @Nullable String password,
                                          @Nullable String email,
                                          @Nullable String displayName,
                                          @Nullable Map extraInfo) {

        return mService.createNewUser(username, password, displayName, email, mGson.toJson(extraInfo));
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
     * @param cb This callback will receive a User in {@link KickflipCallback#onSuccess(Object)}
     *           or an Exception {@link KickflipCallback#onError(KickflipException)}.
     */
    @DebugLog
    public void createNewUser(@Nullable final KickflipCallback cb) {

        final String password = generateRandomPassword();

        mService.createNewUser(password)
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "Failed to create user ", throwable);
                    }
                })
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        if (cb != null) cb.onSuccess(user);
                    }
                });
    }

    /**
     * Login an exiting Kickflip User and make it active.
     *
     * @param username The Kickflip user's username
     * @param password The Kickflip user's password
     * @param cb       This callback will receive a User in {@link KickflipCallback#onSuccess(Object)}
     *                 or an Exception {@link KickflipCallback#onError(KickflipException)}.
     */
    @DebugLog
    public void loginUser(@NonNull String username,
                          @NonNull final String password,
                          @Nullable final KickflipCallback cb) {

        checkNotNull(username);
        checkNotNull(password);

        mService.loginUser(username, password)
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "Failed to login user ", throwable);
                    }
                })
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        if (cb != null) cb.onSuccess(user);
                    }
                });
    }

    /**
     * Login an exiting Kickflip User and make it active.
     *
     * @param username The Kickflip user's username
     * @param password The Kickflip user's password
     * @return An observable for a {@link User}
     */
    @DebugLog
    public Observable<User> loginUser(@NonNull String username,
                                      @NonNull final String password) {

        checkNotNull(username);
        checkNotNull(password);

        return mService.loginUser(username, password);
    }

    /**
     * Set the current active user's meta info. Pass a null argument to leave it as-is.
     *
     * @param newPassword the user's new password
     * @param email       the user's new email address
     * @param displayName The desired display name
     * @param extraInfo   Arbitrary String data to associate with this user.
     * @param cb          This callback will receive a User in {@link KickflipCallback#onSuccess(Object)}
     *                    or an Exception {@link KickflipCallback#onError(KickflipException)}.
     */
    @DebugLog
    public void setUserInfo(@Nullable final String newPassword,
                            @Nullable String email,
                            @Nullable String displayName,
                            @Nullable Map extraInfo,
                            @Nullable final KickflipCallback cb) {

        mService.setUserInfo(newPassword, email, displayName, mGson.toJson(extraInfo))
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "Failed to update user ", throwable);
                    }
                })
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        if (cb != null) cb.onSuccess(user);
                    }
                });
    }

    /**
     * Get public user info
     *
     * @param username The Kickflip user's username
     * @param cb       This callback will receive a User in {@link KickflipCallback#onSuccess(Object)}
     *                 or an Exception {@link KickflipCallback#onError(KickflipException)}.
     */
    @DebugLog
    public void getUserInfo(@NonNull String username,
                            @Nullable final KickflipCallback<User> cb) {

        checkNotNull(username);

        mService.getUserInfo(username)
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        String failMsg = "Failed to get user. Reason " + throwable.getMessage();
                        Log.e(TAG, failMsg, throwable);
                        if (cb != null) cb.onError(new KickflipException(failMsg, 1));
                    }
                })
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        if (cb != null) cb.onSuccess(user);
                    }
                });
    }

    /**
     * Get public user info
     *
     * @param username The Kickflip user's username
     * @return An Observable for the requested User
     */
    @DebugLog
    public Observable<User> getUserInfo(@NonNull String username) {

        checkNotNull(username);

        return mService.getUserInfo(username);
    }


    /**
     * Start a new Stream. Must be called after
     * {@link KickflipApiClient#createNewUser(KickflipCallback)}
     * Delivers stream endpoint destination data via a {@link KickflipCallback}.
     *
     * @param cb This callback will receive a Stream subclass in {@link KickflipCallback#onSuccess(Object)}
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, RtmpStream, etc.
     */
    @DebugLog
    public void startStream(@NonNull Stream stream,
                            @Nullable final KickflipCallback<Stream> cb) {

        checkNotNull(stream);
        startStreamWithUser(getActiveUser(), stream, cb);
    }

    /**
     * Start a new Stream owned by the given User. Must be called after
     * {@link KickflipApiClient#createNewUser(KickflipCallback)}
     * Delivers stream endpoint destination data via a {@link KickflipCallback}.
     *
     * @param user The Kickflip User on whose behalf this request is performed.
     * @param cb   This callback will receive a Stream subclass in {@link KickflipCallback#onSuccess(Object)}
     *             depending on the Kickflip account type. Implementors should
     *             check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    @DebugLog
    private void startStreamWithUser(@NonNull User user,
                                     @NonNull Stream stream,
                                     @Nullable final KickflipCallback<Stream> cb) {
        checkNotNull(user);
        checkNotNull(stream);

        mService.startStream(user.getUUID(),
                stream.isPrivate(),
                stream.getTitle(),
                stream.getDescription(),
                stream.getExtraInfoString())
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        String failMsg = "Failed to start stream. Reason: " + throwable.getMessage();
                        Log.e(TAG, failMsg, throwable);
                        if (cb != null) cb.onError(new KickflipException(failMsg, 1));
                    }
                })
                .subscribe(new Action1<Stream>() {
                    @Override
                    public void call(Stream stream) {
                        if (cb != null) cb.onSuccess(stream);
                    }
                });
    }

    /**
     * Stop a Stream. Must be called after
     * {@link KickflipApiClient#createNewUser(KickflipCallback)} and
     * {@link KickflipApiClient#startStream(Stream, KickflipCallback)}
     *
     * @param cb This callback will receive a Stream subclass in {@link KickflipCallback#onSuccess(Object)}}
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, etc.
     */
    @DebugLog
    public void stopStream(@NonNull Stream stream,
                           @Nullable final KickflipCallback<Stream> cb) {
        stopStream(getActiveUser(), stream, cb);
    }

    /**
     * Stop a Stream owned by the given Kickflip User.
     *
     * @param cb This callback will receive a Stream subclass in #onSuccess(response)
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, etc.
     */
    @DebugLog
    private void stopStream(@NonNull User user,
                            @NonNull Stream stream,
                            @Nullable final KickflipCallback<Stream> cb) {

        checkNotNull(user);
        checkNotNull(stream);
        // TODO: Add start / stop lat lon to Stream?

        mService.stopStream(stream.getStreamId(),
                user.getUUID(),
                stream.getLatitude(),
                stream.getLongitude())
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        String failMsg = "Failed to get user. Reason: " + throwable.getMessage();
                        Log.e(TAG, failMsg, throwable);
                        if (cb != null) cb.onError(new KickflipException(failMsg, 1));
                    }
                })
                .subscribe(new Action1<Stream>() {
                    @Override
                    public void call(Stream stream) {
                        if (cb != null) cb.onSuccess(stream);
                    }
                });
    }

    /**
     * Send Stream Metadata for a {@link Stream}.
     * The target Stream must be owned by the User created with {@link KickflipApiClient#createNewUser(KickflipCallback)}
     * from this KickflipApiClient.
     *
     * @param stream the {@link Stream} to get Meta data for
     * @param cb     A callback to receive the updated Stream upon request completion
     */
    @DebugLog
    public void setStreamInfo(@NonNull Stream stream,
                              @Nullable final KickflipCallback<Stream> cb) {

        checkNotNull(stream);

        mService.setStreamInfo(stream,
                getActiveUser().getUUID())
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        String failMsg = "Failed to set stream info. Reason: " + throwable.getMessage();
                        Log.e(TAG, "Failed to set stream info ", throwable);
                    }
                })
                .subscribe(new Action1<Stream>() {
                    @Override
                    public void call(Stream stream) {
                        if (cb != null) cb.onSuccess(stream);
                    }
                });
    }

    /**
     * Get Stream Metadata for a a public {@link Stream}.
     * The target Stream must belong a User of your Kickflip app.
     *
     * @param streamId the stream id to get info for
     * @param cb       A callback to receive the updated Stream upon request completion
     */
    @DebugLog
    public void getStreamInfo(@NonNull String streamId, @Nullable final KickflipCallback<Stream> cb) {
        checkNotNull(streamId);

        mService.getStreamInfo(streamId)
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        String failMsg = "Failed to get stream info. Reason: " + throwable.getMessage();
                        Log.e(TAG, failMsg, throwable);
                        if (cb != null) cb.onError(new KickflipException(failMsg, 1));
                    }
                })
                .subscribe(new Action1<Stream>() {
                    @Override
                    public void call(Stream stream) {
                        if (cb != null) cb.onSuccess(stream);
                    }
                });
    }

    /**
     * Get Stream Metadata for a a public {@link Stream}.
     * The target Stream must belong a User of your Kickflip app.
     *
     * @param streamId the stream id to get info for
     * @return an Observable for the requested {@link Stream}
     */
    @DebugLog
    public Observable<Stream> getStreamInfo(@NonNull String streamId) {
        checkNotNull(streamId);

        return mService.getStreamInfo(streamId);
    }

    /**
     * Flag a {@link Stream}. Used when the active Kickflip User does not own the Stream.
     * <p/>
     * To delete a recording the active Kickflip User owns, use
     * {@link KickflipApiClient#setStreamInfo(Stream, KickflipCallback)}
     *
     * @param stream The Stream to flag.
     * @param cb     A callback to receive the result of the flagging operation.
     */
    @DebugLog
    public void flagStream(@NonNull Stream stream, @Nullable final KickflipCallback cb) {
        checkNotNull(stream);

        mService.flagStream(stream.getStreamId(), getActiveUser().getUUID())
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        String failMsg = "Failed to flag stream. Reason: " + throwable.getMessage();
                        Log.e(TAG, failMsg, throwable);
                        if (cb != null) cb.onError(new KickflipException(failMsg, 1));
                    }
                })
                .subscribe(new Action1<Stream>() {
                    @Override
                    public void call(Stream stream) {
                        if (cb != null) cb.onSuccess(stream);
                    }
                });
    }

    /**
     * Get a List of {@link Stream} objects created by the given Kickflip User.
     *
     * @param username the target Kickflip username
     * @param cb       A callback to receive the resulting List of Streams
     */
    @DebugLog
    public void getStreamsByUsername(@NonNull String username,
                                     int pageNumber,
                                     int itemsPerPage,
                                     @Nullable final KickflipCallback cb) {
        checkNotNull(username);
        mService.getStreamsByUser(getActiveUser().getUUID(),
                username, itemsPerPage, pageNumber)
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        String failMsg = "Failed to get streams by user. Reason: " + throwable.getMessage();
                        Log.e(TAG, failMsg, throwable);
                        if (cb != null) cb.onError(new KickflipException(failMsg, 1));
                    }
                })
                .subscribe(new Action1<StreamList>() {
                    @Override
                    public void call(StreamList streams) {
                        if (cb != null) cb.onSuccess(streams);
                    }
                });
    }

    /**
     * Get a List of {@link Stream}s containing a keyword.
     * <p/>
     * This method searches all public recordings made by Users of your Kickflip app.
     *
     * @param keyword The String keyword to query
     * @param cb      A callback to receive the resulting List of Streams
     */
    @DebugLog
    public void getStreamsByKeyword(@NonNull String keyword, int pageNumber, int itemsPerPage,
                                    @Nullable final KickflipCallback cb) {
        checkNotNull(keyword);
        mService.getStreamsByKeyword(getActiveUser().getUUID(),
                keyword, itemsPerPage, pageNumber)
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        String failMsg = "Failed to get streams by keyword. Reason: " + throwable.getMessage();
                        Log.e(TAG, failMsg, throwable);
                        if (cb != null) cb.onError(new KickflipException(failMsg, 1));
                    }
                })
                .subscribe(new Action1<StreamList>() {
                    @Override
                    public void call(StreamList streams) {
                        if (cb != null) cb.onSuccess(streams);
                    }
                });
    }

    /**
     * Get a List of {@link Stream}s near a geographic location.
     * <p/>
     * This method searches all public recordings made by Users of your Kickflip app.
     *
     * @param location The target Location
     * @param radius   The target Radius in meters
     * @param cb       A callback to receive the resulting List of Streams
     */
    @DebugLog
    public void getStreamsByLocation(@NonNull Location location, int radius, int pageNumber, int itemsPerPage,
                                     @Nullable final KickflipCallback cb) {
        checkNotNull(location);
        mService.getStreamsByLocation(getActiveUser().getUUID(),
                location.getLatitude(), location.getLongitude(), radius, itemsPerPage, pageNumber)
                .subscribeOn(AndroidSchedulers.handlerThread(mCallbackHandler))
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        String failMsg = "Failed to get streams by geo. Reason: " + throwable.getMessage();
                        Log.e(TAG, failMsg, throwable);
                        if (cb != null) cb.onError(new KickflipException(failMsg, 1));
                    }
                })
                .subscribe(new Action1<StreamList>() {
                    @Override
                    public void call(StreamList streams) {
                        if (cb != null) cb.onSuccess(streams);
                    }
                });
    }

    /**
     * Get the current active Kickflip User. If no User has been created, returns null.
     * <p/>
     * This will be the User created on the last call to
     * {@link KickflipApiClient#createNewUser(KickflipCallback)}
     * <p/>
     * The result of this function is guaranteed to be not null if {@link #create(Context, String, String)}
     * succeeds
     *
     * @return
     */
    @DebugLog
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

    /**
     * Check if a Stream is owned by the active Kickflip User.
     *
     * @param stream the Stream to test.
     * @return true if the active Kickflip User owns the Stream. false otherwise.
     */
    public boolean activeUserOwnsStream(Stream stream) {
        return getActiveUser().getName().compareTo(stream.getOwnerName()) == 0;
    }

    @DebugLog
    private void storeUser(User user, String password) {
        getStorage().edit()
                .putString("app_name", user.getApp())
                .putString("name", user.getName())
                .putString("password", password)
                .putString("uuid", user.getUUID())
                .apply();
    }

    private SharedPreferences getStorage() {
        return mContext.getSharedPreferences("kfsw", Context.MODE_PRIVATE);
    }

    private String generateRandomPassword() {
        return new BigInteger(130, new SecureRandom()).toString(32);
    }

}
