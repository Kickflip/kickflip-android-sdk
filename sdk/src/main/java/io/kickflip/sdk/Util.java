package io.kickflip.sdk;

import android.content.Context;
import android.os.Build;
import android.text.format.DateUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import io.kickflip.sdk.av.SessionConfig;

/**
 * Created by David Brodsky on 3/20/14.
 */
public class Util {
    //"04/03/2014 23:41:37",
    private static SimpleDateFormat mMachineSdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
    ;
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

    public static SessionConfig create720pHLSSessionConfig(Context context) {
        HashMap<String, String> extraData = new HashMap<>();
        extraData.put("key", "value");

        String outputPath = new File(context.getFilesDir(), "index.m3u8").getAbsolutePath();
        // Note SessionConfig actually places each recording in a unique folder
        // e.g: if you pass /sdcard/test/index.m3u8, your recording will be in
        //                  /sdcard/test/<some-uuid>/index.m3u8
        // You can query the actual output file location with SessionConfig#getOutputPath() or the containing directory
        // with SessionConfig#getOutputDirectory() after construction.
        SessionConfig config = new SessionConfig.Builder(outputPath)
                .withTitle(Util.getHumanDateString())
                .withDescription("A live stream!")
                .withAdaptiveStreaming(true)
                .withVideoResolution(1280, 720)
                .withVideoBitrate(2 * 1000 * 1000)
                .withAudioBitrate(192 * 1000)
                .withExtraInfo(extraData)
                .withPrivateVisibility(false)
                .withLocation(true)
                .build();
        return config;
    }

    /**
     * Create a {@link io.kickflip.sdk.av.SessionConfig}
     * corresponding to a 420p video stream
     *
     * @param context the host application Context. Used to access Internal Storage
     * @return the resulting SessionConfig
     */
    public static SessionConfig create420pSessionConfig(Context context, String targer) {
        String outputPath = new File(context.getFilesDir(), "index.m3u8").getAbsolutePath();
        SessionConfig config = new SessionConfig.Builder(outputPath)
                .withTitle(Util.getHumanDateString())
                .withVideoBitrate(1 * 1000 * 1000)
                .withPrivateVisibility(false)
                .withLocation(true)
                .withVideoResolution(720, 480)
                .build();
        return config;
    }
}
