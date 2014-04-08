package io.kickflip.sdk.av;

import android.os.AsyncTask;
import android.util.Log;

import net.chilicat.m3u8.ParseException;
import net.chilicat.m3u8.Playlist;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by David Brodsky on 4/8/14.
 */
public class M3u8Parser {
    public static final String TAG = "M3u8Parser";

    public interface M3u8ParserCallback{
        public void onSuccess(Playlist playlist);
        public void onError(Exception e);
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
