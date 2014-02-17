package io.kickflip.sample;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

public class MainFragment extends Fragment {
    private static final String TAG = "MainFragment";
    private MainFragmentInteractionListener mListener;

    TextView mTitleView;
    TextView mSubtitle;
    Button mButton;

    public static enum EVENT { START_BROADCAST };

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }
    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = null;
        if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            root = inflater.inflate(R.layout.fragment_main, container, false);
            root.findViewById(R.id.broadcastButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListener != null)
                        mListener.onFragmentEvent(EVENT.START_BROADCAST);
                }
            });
            mTitleView = (TextView) root.findViewById(R.id.title);
            mSubtitle = (TextView) root.findViewById(R.id.subtitle);
            mButton = (Button) root.findViewById(R.id.broadcastButton);
            fadeInTitleAndSub();
        }
        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        try {
            mListener = (MainFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainFragmentInteractionListener");
        }
    }

    void fadeInTitleAndSub(){
        Log.i(TAG, "Fading in title & sub");
        Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fadein);
        fadeInAnimation.setStartOffset(50);
        mTitleView.startAnimation(fadeInAnimation);
        fadeInSub();
    }

    void fadeInSub(){
        Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fadein);
        fadeInAnimation.setStartOffset(100);
        mSubtitle.startAnimation(fadeInAnimation);
        fadeInBtn();
    }

    void fadeInBtn(){
        Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fadein);
        fadeInAnimation.setStartOffset(200);
        mButton.startAnimation(fadeInAnimation);
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


}
