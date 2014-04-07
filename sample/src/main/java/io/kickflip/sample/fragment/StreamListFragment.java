package io.kickflip.sample.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import io.kickflip.sample.R;
import io.kickflip.sample.SECRETS;
import io.kickflip.sample.adapter.StreamAdapter;
import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.api.json.StreamList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class StreamListFragment extends Fragment implements AbsListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    public static final String TAG = "StreamListFragment";
    private static final boolean VERBOSE = true;

    private StreamListFragmenListener mListener;
    private SwipeRefreshLayout mSwipeLayout;
    private KickflipApiClient mKickflip;
    private List<Stream> mStreams;
    private boolean mRefreshing;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private StreamAdapter mAdapter;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StreamListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKickflip = new KickflipApiClient(getActivity(), SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                getStreams();
            }

            @Override
            public void onError(Object response) {
                showNetworkError();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mKickflip.credentialsAcquired()) {
            getStreams();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stream, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        // Why does this selection remain if I long press, release
        // without activating onListItemClick?
        //mListView.setSelector(R.drawable.stream_list_selector_overlay);
        //mListView.setDrawSelectorOnTop(true);

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.refreshLayout);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorScheme(R.color.kickflip_green,
                R.color.kickflip_green_shade_2,
                R.color.kickflip_green_shade_3,
                R.color.kickflip_green_shade_4);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (StreamListFragmenListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement StreamListFragmenListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Stream stream = mAdapter.getItem(position);
        mListener.onStreamPlaybackRequested(stream.getStreamUrl());
    }

    private void getStreams() {
        mRefreshing = true;
        mKickflip.getBroadcastsByKeyword(mKickflip.getCachedUser(), null, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (VERBOSE) Log.i("API", "request succeeded " + response);
                if (getActivity() != null) {
                    mStreams = ((StreamList) response).getStreams();
                    if (mStreams.size() == 0) {
                        showNoBroadcasts();
                    } else {
                        Collections.sort(mStreams);
                        mAdapter = new StreamAdapter(getActivity(), mStreams);
                        mListView.setAdapter(mAdapter);
                    }
                }
                mSwipeLayout.setRefreshing(false);
                mRefreshing = false;
            }

            @Override
            public void onError(Object response) {
                if (VERBOSE) Log.i("API", "request failed " + response);
                if (getActivity() != null) {
                    showNetworkError();
                }
                mSwipeLayout.setRefreshing(false);
                mRefreshing = false;
            }
        });
    }

    /**
     * Inform the user that a network error has occured
     */
    public void showNetworkError() {
        setEmptyListViewText(getString(R.string.no_network));
    }

    /**
     * Inform the user that no broadcasts were found
     */
    public void showNoBroadcasts() {
        setEmptyListViewText(getString(R.string.no_broadcasts));
    }

    /**
     * If the ListView is hidden, show the
     *
     * @param text
     */
    private void setEmptyListViewText(String text) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(text);
        }
    }

    @Override
    public void onRefresh() {
        if (!mRefreshing) {
            getStreams();
        }

    }

    public interface StreamListFragmenListener {
        public void onStreamPlaybackRequested(String url);
    }

}
