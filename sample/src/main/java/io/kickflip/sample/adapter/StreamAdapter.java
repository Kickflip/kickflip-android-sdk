package io.kickflip.sample.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import io.kickflip.sample.R;
import io.kickflip.sdk.api.json.Stream;

/**
 * StreamAdapter connects a List of Streams
 * to an Adapter backed view like ListView or GridView
 *
 */
public class StreamAdapter extends ArrayAdapter<Stream> {

    public static final int LAYOUT_ID = R.layout.stream_list_item;

    public StreamAdapter(final Context context, List<Stream> objects) {
        super(context, LAYOUT_ID, objects);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Stream stream = getItem(position);
        ViewHolder holder;
        if (convertView == null) {
            convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(LAYOUT_ID, null);
            holder = new ViewHolder();
            holder.imageView = (ImageView) convertView.findViewById(R.id.image);
            holder.titleView = (TextView) convertView.findViewById(R.id.title);
            holder.subTitleView = (TextView) convertView.findViewById(R.id.subTitle);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (stream.getThumbnailUrl() != null && stream.getThumbnailUrl().compareTo("") != 0) {
            Picasso.with(getContext()).load(stream.getThumbnailUrl()).into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.play);
        }
        holder.titleView.setText(stream.getTitle());
        holder.subTitleView.setText(stream.getCity());

        return convertView;
    }

    public static class ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView subTitleView;
    }
}
