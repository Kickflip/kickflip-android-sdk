package io.kickflip.sample;

import android.os.Build;

/**
 * Created by David Brodsky on 3/20/14.
 */
public class Util {

    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }
}
