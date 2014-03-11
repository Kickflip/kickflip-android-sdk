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
 * Created by davidbrodsky on 3/11/14.
 */
public class StreamAdapter extends ArrayAdapter<Stream> {

    public static final int LAYOUT_ID = R.layout.stream_list_item;

    public StreamAdapter(final Context context, List<Stream> objects) {
        super(context, LAYOUT_ID, objects);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Stream stream = getItem(position);
        if (convertView == null) {
            convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(LAYOUT_ID, null);
        }

        ImageView imageView = (ImageView) convertView.findViewById(R.id.image);
        TextView titleView = (TextView) convertView.findViewById(R.id.title);
        TextView subTitleView = (TextView) convertView.findViewById(R.id.subTitle);

        if(stream.getThumbnailUrl() != null && stream.getThumbnailUrl().compareTo("") != 0 ){
            Picasso.with(getContext()).load(stream.getThumbnailUrl()).into(imageView);
        } else {
            imageView.setImageResource(R.drawable.play);
        }
        titleView.setText(stream.getTitle());
        subTitleView.setText(stream.getCity());

        return convertView;
    }
}
