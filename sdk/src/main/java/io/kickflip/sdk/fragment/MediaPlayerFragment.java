package io.kickflip.sdk.fragment;


import android.app.Fragment;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.Playlist;

import java.io.IOException;

import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.R;
import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.av.M3u8Parser;
import io.kickflip.sdk.exception.KickflipException;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * MediaPlayerFragment demonstrates playing an HLS Stream, and fetching
 * stream metadata via the .m3u8 manifest to decorate the display for Live streams.
 * <p/>
 * Note : {@link Kickflip#setup(Context, String, String)} or {@link Kickflip#setup(Context, String, String, KickflipCallback)}
 * must be called before this Fragment is added to any Activity! We can remove this requirement by reading Kickflip credentials
 * from an xml resource...
 */
public class MediaPlayerFragment extends Fragment implements TextureView.SurfaceTextureListener, MediaController.MediaPlayerControl {
    private static final String TAG = "MediaPlayerFragment";
    private static final boolean VERBOSE = false;
    private static final String ARG_URL = "url";
    private KickflipApiClient mKickflip;

    private ProgressBar mProgress;
    private TextureView mTextureView;
    private TextView mLiveLabel;
    private MediaPlayer mMediaPlayer;
    private MediaController mMediaController;
    private String mMediaUrl;

    // M3u8 Media properties inferred from .m3u8
    private int mDurationMs;
    private boolean mIsLive;

    private Surface mSurface;

    private M3u8Parser.M3u8ParserCallback m3u8ParserCallback = new M3u8Parser.M3u8ParserCallback() {
        @Override
        public void onSuccess(Playlist playlist) {
            updateUIWithM3u8Playlist(playlist);
            setupMediaPlayer(mSurface);
        }

        @Override
        public void onError(Exception e) {
            if (VERBOSE) Log.i(TAG, "m3u8 parse failed " + e.getMessage());
        }
    };

    private View.OnTouchListener mTextureViewTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mMediaController != null && isResumed()) {
                mMediaController.show();
            }
            return false;
        }
    };

    public static MediaPlayerFragment newInstance(String mediaUrl) {
        MediaPlayerFragment fragment = new MediaPlayerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, mediaUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public MediaPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMediaUrl = getArguments().getString(ARG_URL);
            if (Kickflip.isKickflipUrl(Uri.parse(mMediaUrl))) {

                Kickflip.getApiClient(getActivity())
                        .doOnNext(new Action1<KickflipApiClient>() {
                            @Override
                            public void call(KickflipApiClient kickflipApiClient) {
                                mKickflip = kickflipApiClient;
                            }
                        })
                        .flatMap(new Func1<KickflipApiClient, Observable<Stream>>() {
                            @Override
                            public Observable<Stream> call(KickflipApiClient kickflipApiClient) {
                                String streamId = Kickflip.getStreamIdFromKickflipUrl(Uri.parse(mMediaUrl));
                                return kickflipApiClient.getStreamInfo(streamId);
                            }
                        })
                        .subscribe(new Action1<Stream>() {
                            @Override
                            public void call(Stream stream) {
                                Log.i(TAG, "got kickflip stream meta: " + stream.getStreamUrl());
                                mMediaUrl = stream.getStreamUrl();
                                parseM3u8FromMediaUrl();
                            }
                        });
            } else if (mMediaUrl.substring(mMediaUrl.lastIndexOf(".") + 1).equals("m3u8")) {
                parseM3u8FromMediaUrl();
            } else {
                throw new IllegalArgumentException("Unknown HLS media url format: " + mMediaUrl);
            }
        }
    }

    private void updateUIWithM3u8Playlist(Playlist playlist) {
        int durationSec = 0;
        for (Element element : playlist.getElements()) {
            durationSec += element.getDuration();
        }
        mIsLive = !playlist.isEndSet();
        mDurationMs = durationSec * 1000;

        if (mIsLive) {
            mLiveLabel.setVisibility(View.VISIBLE);
        }
    }

    private void setupMediaPlayer(Surface displaySurface) {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setSurface(displaySurface);
            mMediaPlayer.setDataSource(mMediaUrl);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (VERBOSE) Log.i(TAG, "media player prepared");
                    mProgress.setVisibility(View.GONE);
                    mMediaController.setEnabled(true);
                    mTextureView.setOnTouchListener(mTextureViewTouchListener);
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (getActivity() != null) {
                        if (VERBOSE) Log.i(TAG, "media player complete. finishing");
                        getActivity().finish();
                    }
                }
            });

            mMediaController = new MediaController(getActivity());
            mMediaController.setAnchorView(mTextureView);
            mMediaController.setMediaPlayer(this);

            mMediaPlayer.prepareAsync();
        } catch (IOException ioe) {

        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_media_player, container, false);
        if (root != null) {
            mTextureView = (TextureView) root.findViewById(R.id.textureView);
            mTextureView.setSurfaceTextureListener(this);
            mProgress = (ProgressBar) root.findViewById(R.id.progress);
            mLiveLabel = (TextView) root.findViewById(R.id.liveLabel);
        }
        return root;
    }

    private void parseM3u8FromMediaUrl() {
        M3u8Parser.parseM3u8(mMediaUrl, m3u8ParserCallback);
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        mSurface = new Surface(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mMediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return !mIsLive;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
