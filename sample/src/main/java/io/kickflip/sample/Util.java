package io.kickflip.sample;

import android.os.Build;
import android.text.format.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by David Brodsky on 3/20/14.
 */
public class Util {
    //"04/03/2014 23:41:37",
    private static SimpleDateFormat mMachineSdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);;
    private static SimpleDateFormat mHumanSdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);

    static {
        mMachineSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static String getHumanDateString() {
        return mHumanSdf.format(new Date());
    }

    public static String getHumanRelativeDateStringFromString(String machineDateStr) {
        String result = null;
        try {
            result = DateUtils.getRelativeTimeSpanString(mMachineSdf.parse(machineDateStr).getTime()).toString();
            result = result.replace("in 0 minutes", "just now");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }
}
