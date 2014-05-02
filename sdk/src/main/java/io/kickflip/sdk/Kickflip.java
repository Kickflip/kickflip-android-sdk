package io.kickflip.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;

import com.google.common.eventbus.EventBus;

import java.io.IOException;

import io.kickflip.sdk.activity.BroadcastActivity;
import io.kickflip.sdk.activity.MediaPlayerActivity;
import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.av.BroadcastListener;
import io.kickflip.sdk.av.SessionConfig;
import io.kickflip.sdk.event.StreamLocationAddedEvent;
import io.kickflip.sdk.location.DeviceLocation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static convenience methods for interacting with Kickflip.
 */
public class Kickflip {

    private static String sApiKey;
    private static String sApiSecret;

    private static KickflipApiClient sKickflip;

    // Per-Stream settings
    private static SessionConfig sSessionConfig;          // Absolute path to root storage location
    private static BroadcastListener sBroadcastListener;

    /**
     * Register with Kickflip, creating a new user identity per app installation.
     *
     * @param context the host application's {@link android.content.Context}
     * @param key     your Kickflip Client Key
     * @param secret  your Kickflip Client Secret
     * @return a {@link io.kickflip.sdk.api.KickflipApiClient} used to perform actions on behalf of a
     * {@link io.kickflip.sdk.api.json.User}.
     */
    public static KickflipApiClient setup(Context context, String key, String secret) {
        setApiCredentials(key, secret);
        return getApiClient(context, null);
    }

    /**
     * Register with Kickflip, creating a new user identity per app installation.
     *
     * @param context the host application's {@link android.content.Context}
     * @param key     your Kickflip Client Key
     * @param secret  your Kickflip Client Secret
     * @param cb      A callback to be invoked when Kickflip user credentials are available.
     * @return a {@link io.kickflip.sdk.api.KickflipApiClient} used to perform actions on behalf of
     * a {@link io.kickflip.sdk.api.json.User}.
     */
    public static KickflipApiClient setup(Context context, String key, String secret, KickflipCallback cb) {
        setApiCredentials(key, secret);
        return getApiClient(context, cb);
    }

    private static void setApiCredentials(String key, String secret) {
        sApiKey = key;
        sApiSecret = secret;
    }

    /**
     * Start {@link io.kickflip.sdk.activity.BroadcastActivity}. This Activity
     * facilitates control over a single live broadcast.
     * <p/>
     * <b>Must be called after {@link Kickflip#setup(android.content.Context, String, String)} or
     * {@link Kickflip#setup(android.content.Context, String, String, io.kickflip.sdk.api.KickflipCallback)}.</b>
     *
     * @param host     the host {@link android.app.Activity} initiating this action
     * @param listener an optional {@link io.kickflip.sdk.av.BroadcastListener} to be notified on
     *                 broadcast events
     */
    public static void startBroadcastActivity(Activity host, BroadcastListener listener) {
        checkNotNull(listener, host.getString(R.string.error_no_broadcastlistener));
        checkNotNull(sSessionConfig, host.getString(R.string.error_no_recorderconfig));
        checkNotNull(sApiKey);
        checkNotNull(sApiSecret);
        sBroadcastListener = listener;
        Intent broadcastIntent = new Intent(host, BroadcastActivity.class);
        broadcastIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        host.startActivity(broadcastIntent);
    }

    /**
     * Start {@link io.kickflip.sdk.activity.MediaPlayerActivity}. This Activity
     * facilitates playing back a Kickflip broadcast.
     * <p/>
     * <b>Must be called after {@link Kickflip#setup(android.content.Context, String, String)} or
     * {@link Kickflip#setup(android.content.Context, String, String, io.kickflip.sdk.api.KickflipCallback)}.</b>
     *
     * @param host      the host {@link android.app.Activity} initiating this action
     * @param streamUrl a path of format https://kickflip.io/<stream_id> or https://xxx.xxx/xxx.m3u8
     * @param newTask   Whether this Activity should be started as part of a new task. If so, when this Activity finishes
     *                  the host application will be concluded.
     */
    public static void startMediaPlayerActivity(Activity host, String streamUrl, boolean newTask) {
        Intent playbackIntent = new Intent(host, MediaPlayerActivity.class);
        playbackIntent.putExtra("mediaUrl", streamUrl);
        if (newTask) {
            playbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        host.startActivity(playbackIntent);
    }

    /**
     * Convenience method for attaching the current reverse geocoded device location to a given
     * {@link io.kickflip.sdk.api.json.Stream}
     *
     * @param context  the host application {@link android.content.Context}
     * @param stream   the {@link io.kickflip.sdk.api.json.Stream} to attach location to
     * @param eventBus an {@link com.google.common.eventbus.EventBus} to be notified of the complete action
     */
    public static void addLocationToStream(final Context context, final Stream stream, final EventBus eventBus) {
        DeviceLocation.getLastKnownLocation(context, false, new DeviceLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                stream.setLatitude(location.getLatitude());
                stream.setLongitude(location.getLongitude());

                try {
                    Geocoder geocoder = new Geocoder(context);
                    Address address = geocoder.getFromLocation(location.getLatitude(),
                            location.getLongitude(), 1).get(0);
                    stream.setCity(address.getLocality());
                    stream.setCountry(address.getCountryName());
                    stream.setState(address.getAdminArea());
                    if (eventBus != null) {
                        eventBus.post(new StreamLocationAddedEvent());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * Get the {@link io.kickflip.sdk.av.BroadcastListener} to be notified on broadcast events.
     */
    public static BroadcastListener getBroadcastListener() {
        return sBroadcastListener;
    }

    /**
     * Set a {@link io.kickflip.sdk.av.BroadcastListener} to be notified on broadcast events.
     *
     * @param listener a {@link io.kickflip.sdk.av.BroadcastListener}
     */
    public static void setBroadcastListener(BroadcastListener listener) {
        sBroadcastListener = listener;
    }

    /**
     * Get the provided Kickflip Client Key
     *
     * @return the provided Kickflip Client Key
     */
    public static String getApiKey() {
        return sApiKey;
    }

    /**
     * Get the provided Kickflip Client Secret
     *
     * @return the provided Kickflip Client Secret
     */
    public static String getApiSecret() {
        return sApiSecret;
    }

    /**
     * Return the {@link io.kickflip.sdk.av.SessionConfig} responsible for configuring this broadcast.
     *
     * @return the {@link io.kickflip.sdk.av.SessionConfig} responsible for configuring this broadcast.
     */
    public static SessionConfig getSessionConfig() {
        return sSessionConfig;
    }

    /**
     * Set the {@link io.kickflip.sdk.av.SessionConfig} responsible for configuring this broadcast.
     *
     * @param config the {@link io.kickflip.sdk.av.SessionConfig} responsible for configuring this broadcast.
     */
    public static void setSessionConfig(SessionConfig config) {
        sSessionConfig = config;
    }

    /**
     * Check whether credentials required for broadcast are provided
     *
     * @return true if credentials required for broadcast are provided. false otherwise
     */
    public static boolean readyToBroadcast() {
        return sApiKey != null && sApiSecret != null && sSessionConfig != null;
    }

    /**
     * Return whether the given Uri belongs to the kickflip.io authority.
     *
     * @param uri uri to test
     * @return true if the uri is of the kickflip.io authority.
     */
    public static boolean isKickflipUrl(Uri uri) {
        return uri != null && uri.getAuthority().contains("kickflip.io");
    }

    /**
     * Given a Kickflip.io url, return the stream id.
     * <p/>
     * e.g: https://kickflip.io/39df392c-4afe-4bf5-9583-acccd8212277/ returns
     * "39df392c-4afe-4bf5-9583-acccd8212277"
     *
     * @param uri the uri to test
     * @return the last path segment of the given uri, corresponding to the Kickflip {@link Stream#mStreamId}
     */
    public static String getStreamIdFromKickflipUrl(Uri uri) {
        if (uri == null) throw new IllegalArgumentException("uri cannot be null");
        return uri.getLastPathSegment().toString();
    }

    /**
     * Create a new instance of the KickflipApiClient if one hasn't
     * yet been created, or the provided API keys don't match
     * the existing client.
     *
     * @param context  the context of the host application
     * @return
     */
    public static KickflipApiClient getApiClient(Context context) {
        return getApiClient(context, null);
    }

    /**
     * Create a new instance of the KickflipApiClient if one hasn't
     * yet been created, or the provided API keys don't match
     * the existing client.
     *
     * @param context  the context of the host application
     * @param callback an optional callback to be notified with the Kickflip user
     *                 corresponding to the provided API keys.
     * @return
     */
    public static KickflipApiClient getApiClient(Context context, KickflipCallback callback) {
        checkNotNull(sApiKey);
        checkNotNull(sApiSecret);
        if (sKickflip == null || !sKickflip.getConfig().getClientId().equals(sApiKey)) {
            sKickflip = new KickflipApiClient(context, sApiKey, sApiSecret, callback);
        } else if (callback != null) {
            callback.onSuccess(sKickflip.getActiveUser());
        }
        return sKickflip;
    }

}
