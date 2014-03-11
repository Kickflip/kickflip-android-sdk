package io.kickflip.sdk.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

//	Thanks, Fedor!
// TODO: Modify getLocation to take callback
public class DeviceLocation {
    private static final String TAG = "DeviceLocation";
    private static final float ACCURATE_LOCATION_THRESHOLD_METERS = 100;
    Timer timer1;
    LocationManager lm;
    LocationResult locationResult;
    boolean gps_enabled = false;
    boolean network_enabled = false;
    private Location bestLocation;
    private boolean waitForGpsFix;


    public static void getLocation(Context context, boolean waitForGpsFix, final LocationResult cb) {
        DeviceLocation deviceLocation = new DeviceLocation();
        deviceLocation.getLocation(context, cb, waitForGpsFix);
    }

    /**
     * Get the last known location.
     * If one is not available, fetch
     * a fresh location
     *
     * @param context
     * @param waitForGpsFix
     * @param cb
     */
    public static void getLastKnownLocation(Context context, boolean waitForGpsFix, final LocationResult cb) {
        DeviceLocation deviceLocation = new DeviceLocation();

        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location last_loc;
        last_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (last_loc == null)
            last_loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (last_loc != null && cb != null) {
            cb.gotLocation(last_loc);
        } else {
            deviceLocation.getLocation(context, cb, waitForGpsFix);
        }
    }


    public boolean getLocation(Context context, LocationResult result, boolean waitForGpsFix) {
        this.waitForGpsFix = waitForGpsFix;

        //I use LocationResult callback class to pass location value from MyLocation to user code.
        locationResult = result;
        if (lm == null)
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //exceptions will be thrown if provider is not permitted.
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        //don't start listeners if no provider is enabled
        if (!gps_enabled && !network_enabled)
            return false;

        if (gps_enabled)
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
        if (network_enabled)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
        timer1 = new Timer();
        timer1.schedule(new GetBestLocation(), 20000);
        return true;
    }

    public static abstract class LocationResult {
        public abstract void gotLocation(Location location);
    }

    LocationListener locationListenerGps = new LocationListener() {
        public void onLocationChanged(Location location) {
            Log.i(TAG, "got GPS loc accurate to " + String.valueOf(location.getAccuracy()) + "m");
            if (bestLocation == null || bestLocation.getAccuracy() > location.getAccuracy())
                bestLocation = location;

            if (!waitForGpsFix || bestLocation.getAccuracy() < ACCURATE_LOCATION_THRESHOLD_METERS) {
                timer1.cancel();
                locationResult.gotLocation(bestLocation);
                lm.removeUpdates(this);
                lm.removeUpdates(locationListenerNetwork);
            }

        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    class GetBestLocation extends TimerTask {
        @Override
        public void run() {
            Log.i(TAG, "Timer expired before adequate location acquired");
            lm.removeUpdates(locationListenerGps);
            lm.removeUpdates(locationListenerNetwork);

            Location net_loc = null, gps_loc = null;
            if (gps_enabled)
                gps_loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (network_enabled)
                net_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            //if there are both values use the latest one
            if (gps_loc != null && net_loc != null) {
                if (gps_loc.getTime() > net_loc.getTime())
                    locationResult.gotLocation(gps_loc);
                else
                    locationResult.gotLocation(net_loc);
                return;
            }

            if (gps_loc != null) {
                locationResult.gotLocation(gps_loc);
                return;
            }
            if (net_loc != null) {
                locationResult.gotLocation(net_loc);
                return;
            }
            locationResult.gotLocation(null);
        }
    }


    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            Log.i(TAG, "got network loc accurate to " + String.valueOf(location.getAccuracy()) + "m");
            if (bestLocation == null || bestLocation.getAccuracy() > location.getAccuracy())
                bestLocation = location;

            if (!waitForGpsFix || bestLocation.getAccuracy() < ACCURATE_LOCATION_THRESHOLD_METERS) {
                timer1.cancel();
                locationResult.gotLocation(bestLocation);
                lm.removeUpdates(this);
                lm.removeUpdates(locationListenerGps);
            }

        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };


}