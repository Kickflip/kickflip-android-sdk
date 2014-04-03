package io.kickflip.sample;

import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by David Brodsky on 3/20/14.
 */
public class Util {

    private static SimpleDateFormat mSdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);;

    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static String getHumanDateString() {
        return mSdf.format(new Date());
    }
}
