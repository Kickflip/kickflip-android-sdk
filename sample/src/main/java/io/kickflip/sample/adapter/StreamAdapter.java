package io.kickflip.sample.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.kickflip.sample.R;
import io.kickflip.sample.Util;
import io.kickflip.sdk.api.json.Stream;

/**
 * StreamAdapter connects a List of Streams
 * to an Adapter backed view like ListView or GridView
 */
public class StreamAdapter extends ArrayAdapter<Stream> {
    public static final int LAYOUT_ID = R.layout.stream_list_item;
    private StreamAdapterActionListener mActionListener;
    private String mUsername;

    public StreamAdapter(final Context context, List<Stream> objects, StreamAdapterActionListener listener) {
        super(context, LAYOUT_ID, objects);
        mActionListener = listener;
    }

    /**
     * Set a Kickflip username to enable this adapter
     * to stylize user-owned entries appropriately.
     *
     * Should be called before {@link #notifyDataSetChanged()}
     *
     * @param userName the Kickflip username this view should be stylized for
     */
    public void setUserName(String userName) {
        mUsername = userName;
    }

    /**
     * Refresh the entire data structure underlying this adapter,
     * resuming the precise scroll state.
     *
     * @param listView
     * @param streams
     */
    public void refresh(AbsListView listView, List<Stream> streams) {
        Parcelable state = listView.onSaveInstanceState();
        clear();
        addAll(streams);
        notifyDataSetChanged();
        listView.onRestoreInstanceState(state);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Stream stream = getItem(position);
        ViewHolder holder;
        if (convertView == null) {
            convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(LAYOUT_ID, null);
            holder = new ViewHolder();
            holder.imageView = (ImageView) convertView.findViewById(R.id.image);
            holder.titleView = (TextView) convertView.findViewById(R.id.title);
            holder.liveBannerView = (TextView) convertView.findViewById(R.id.liveLabel);
            holder.rightTitleView = (TextView) convertView.findViewById(R.id.rightTitle);
            holder.overflowBtn = (ImageButton) convertView.findViewById(R.id.overflowBtn);
            holder.actions = convertView.findViewById(R.id.actions);
            convertView.setTag(holder);
            convertView.findViewById(R.id.overflowBtn).setOnClickListener(mOverflowBtnClickListener);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        // Hide the stream actions panel
        holder.actions.setVisibility(View.GONE);
        holder.overflowBtn.setTag(position);

        int streamLengthSec = stream.getLengthInSeconds();
        if (streamLengthSec == 0) {
            // A Live Stream
            holder.liveBannerView.setVisibility(View.VISIBLE);
            holder.rightTitleView.setText("started " + Util.getHumanRelativeDateStringFromString(stream.getTimeStarted()));
        } else {
            // A previously ended Stream
            holder.liveBannerView.setVisibility(View.GONE);
            holder.rightTitleView.setText(String.format("%d:%02d",
                    TimeUnit.SECONDS.toMinutes(streamLengthSec),
                    TimeUnit.SECONDS.toSeconds(streamLengthSec) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(streamLengthSec))
            ));
        }

        if (stream.getThumbnailUrl() != null && stream.getThumbnailUrl().compareTo("") != 0) {
            Picasso.with(getContext()).load(stream.getThumbnailUrl()).into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.play);
        }
        holder.titleView.setText(stream.getTitle());

        return convertView;
    }

    public static class ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView liveBannerView;
        TextView rightTitleView;
        ImageButton overflowBtn;
        View actions;
    }

    private View.OnClickListener mOverflowBtnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View overflowBtn) {
            // Toggle the Action container's visibility and set Tags on its subviews
            View listItemParent = ((View)overflowBtn.getParent());
            if (isActionContainerVisible(listItemParent)) {
                hideActionContainer(listItemParent);
            } else {
                View actionContainer = listItemParent.findViewById(R.id.actions);
                if (mUsername != null) {
                    // TODO: Server returns AppUser, not individual user.
                    if (mUsername.compareTo(getItem(((Integer) overflowBtn.getTag())).getOwnerName()) == 0) {
                        ((ImageButton) actionContainer.findViewById(R.id.flagBtn)).setImageResource(R.drawable.ic_trash);
                    } else {
                        ((ImageButton) actionContainer.findViewById(R.id.flagBtn)).setImageResource(R.drawable.ic_red_flag);
                    }
                }
                showActionContainer(listItemParent);
                // Transfer the overflowBtn tag to the two newly revealed buttons
                actionContainer.findViewById(R.id.flagBtn).setTag(overflowBtn.getTag());
                actionContainer.findViewById(R.id.flagBtn).setOnClickListener(mFlagBtnClick);
                actionContainer.findViewById(R.id.shareBtn).setTag(overflowBtn.getTag());
                actionContainer.findViewById(R.id.shareBtn).setOnClickListener(mShareBtnClick);
            }
        }
    };

    private View.OnClickListener mFlagBtnClick = new View.OnClickListener(){

        @Override
        public void onClick(View flagBtn) {
            mActionListener.onFlagButtonClick(getItem((Integer) flagBtn.getTag()));
            hideActionContainer((View) flagBtn.getParent().getParent());
        }
    };

    private View.OnClickListener mShareBtnClick = new View.OnClickListener(){

        @Override
        public void onClick(View shareBtn) {
            mActionListener.onShareButtonClick(getItem((Integer) shareBtn.getTag()));
            hideActionContainer((View) shareBtn.getParent().getParent());
        }
    };

    private boolean isActionContainerVisible(View listItemParent) {
        return listItemParent.findViewById(R.id.actions).getVisibility() == View.VISIBLE;
    }

    private void showActionContainer(View listItemParent) {
        View actionContainer = listItemParent.findViewById(R.id.actions);

        ObjectAnimator imageWashOut = ObjectAnimator.ofFloat(listItemParent.findViewById(R.id.image), "alpha", 1f, 0.4f);
        imageWashOut.setDuration(250);
        imageWashOut.start();

        actionContainer.setVisibility(View.VISIBLE);
    }

    private void hideActionContainer(View listItemParent) {
        View actionContainer = listItemParent.findViewById(R.id.actions);

        ObjectAnimator imageWashOut = ObjectAnimator.ofFloat(listItemParent.findViewById(R.id.image), "alpha", 0.4f, 1.0f);
        imageWashOut.setDuration(250);
        imageWashOut.start();

        actionContainer.setVisibility(View.GONE);
    }

    public static interface StreamAdapterActionListener {
        public void onFlagButtonClick(Stream stream);
        public void onShareButtonClick(Stream stream);
    }

}
