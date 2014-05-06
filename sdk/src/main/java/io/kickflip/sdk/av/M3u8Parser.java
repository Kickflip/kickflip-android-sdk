package io.kickflip.sdk.av;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import net.chilicat.m3u8.ParseException;
import net.chilicat.m3u8.Playlist;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import io.kickflip.sdk.api.KickflipApiClient;

import static io.kickflip.sdk.Kickflip.isKickflipUrl;

/**
 * Created by David Brodsky on 4/8/14.
 * @hide
 */
public class M3u8Parser {
    public static final String TAG = "M3u8Parser";

    public interface M3u8ParserCallback{
        public void onSuccess(Playlist playlist);
        public void onError(Exception e);
    }

    public static void getM3u8FromUrl(KickflipApiClient kickflip, String url, final M3u8ParserCallback cb) {
        if (isKickflipUrl(Uri.parse(url))) {

        } else if(url.substring(url.lastIndexOf(".")+1).equals("m3u8")) {
            parseM3u8(url, cb);
        } else {
            throw new IllegalArgumentException("Url is not an .m3u8 or kickflip.io url");
        }
    }

    public static void parseM3u8(String url, final M3u8ParserCallback cb) {
        new AsyncTask<String, Void, Playlist>() {

            @Override
            protected Playlist doInBackground(String... params) {
                URL url = null;
                try {
                    url = new URL(params[0]);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        String errorMessage =  "Server returned HTTP " + connection.getResponseCode()
                                + " " + connection.getResponseMessage();
                        Log.e(TAG, errorMessage);
                        cb.onError(new IOException(errorMessage));
                        return null;
                    }

                    Playlist playlist =  Playlist.parse(connection.getInputStream());
                    if (playlist != null) {
                        return playlist;
                    } else {
                        cb.onError(new Exception("Unable to Parse playlist"));
                    }

                } catch (ParseException | IOException e) {
                    e.printStackTrace();
                    cb.onError(e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Playlist playlist) {
                if (playlist != null) {
                    cb.onSuccess(playlist);
                }
            }
        }.execute(url);
    }
}
