package io.kickflip.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.google.common.eventbus.EventBus;

import java.io.IOException;

import io.kickflip.sdk.activity.BroadcastActivity;
import io.kickflip.sdk.activity.MediaPlayerActivity;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.av.BroadcastListener;
import io.kickflip.sdk.av.SessionConfig;
import io.kickflip.sdk.events.StreamLocationAddedEvent;
import io.kickflip.sdk.location.DeviceLocation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static convenience methods for interacting with Kickflip's
 * BroadcastActivity and Broadcaster. The static setters will only take effect
 * if called before startBroadcastActivity
 */
public class Kickflip {

    private static String sApiKey;
    private static String sApiSecret;

    // Per-Stream settings
    private static SessionConfig sSessionConfig;          // Absolute path to root storage location

    private static BroadcastListener sBroadcastListener;

    public static void setupWithApiKey(String key, String secret) {
        sApiKey = key;
        sApiSecret = secret;
    }

    public static void startBroadcastActivity(Activity host, BroadcastListener listener) {
        checkNotNull(listener, host.getString(R.string.error_no_broadcastlistener));
        checkNotNull(sSessionConfig, host.getString(R.string.error_no_recorderconfig));
        sBroadcastListener = listener;
        Intent broadcastIntent = new Intent(host, BroadcastActivity.class);
        broadcastIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        host.startActivity(broadcastIntent);
    }

    public static void startMediaPlayerActivity(Activity host, String streamUrl) {
        Intent playbackIntent = new Intent(host, MediaPlayerActivity.class);
        playbackIntent.putExtra("mediaUrl", streamUrl);
        host.startActivity(playbackIntent);
    }

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

    public static BroadcastListener getBroadcastListener() {
        return sBroadcastListener;
    }

    public static void setBroadcastListener(BroadcastListener listener) {
        sBroadcastListener = listener;
    }

    public static String getApiKey() {
        return sApiKey;
    }

    public static String getApiSecret() {
        return sApiSecret;
    }

    public static void setSessionConfig(SessionConfig config) {
        sSessionConfig = config;
    }

    public static SessionConfig getRecorderConfig() {
        return sSessionConfig;
    }

    public static boolean readyToBroadcast() {
        return sApiKey != null && sApiSecret != null && sSessionConfig != null;
    }


}
