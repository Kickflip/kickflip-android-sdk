package io.kickflip.sdk.fragment;


import android.app.Fragment;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import io.kickflip.sdk.R;

public class MediaPlayerFragment extends Fragment {
    private static final String ARG_URL = "url";

    private ProgressBar mProgress;
    private VideoView mVideoView;
    private String mMediaUrl;

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
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        mVideoView.setVideoURI(Uri.parse(mMediaUrl));
        MediaController mediaController = new MediaController(getActivity());
        mediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mediaController);
        mProgress.setVisibility(View.VISIBLE);
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mProgress.setVisibility(View.GONE);
                mVideoView.start();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoView.isPlaying()) {
            mVideoView.stopPlayback();
        }  else {
            mVideoView.suspend();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_media_player, container, false);
        if (root != null) {
            mVideoView = (VideoView) root.findViewById(R.id.videoView);
            mProgress = (ProgressBar) root.findViewById(R.id.progress);
        }
        return root;
    }


}
