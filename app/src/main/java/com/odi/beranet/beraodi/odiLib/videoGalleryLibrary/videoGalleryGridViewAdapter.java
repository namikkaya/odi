package com.odi.beranet.beraodi.odiLib.videoGalleryLibrary;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.odi.beranet.beraodi.R;
import com.odi.beranet.beraodi.models.dataBaseItemModel;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class videoGalleryGridViewAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final Context context;
    private final ArrayList<dataBaseItemModel> list;

    private gridViewHolder holder;

    public videoGalleryGridViewAdapter(Context context, ArrayList<dataBaseItemModel> list) {
        this.context = context;
        this.list = list;
        this.inflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return (dataBaseItemModel)list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.gallery_video_cell_layout, null);

            holder = new gridViewHolder();
            holder.cellText = convertView.findViewById(R.id.cellText);
            holder.cellImage = convertView.findViewById(R.id.galleryVideoCellImageView);
            convertView.setTag(holder);
        }else {
            holder = (gridViewHolder) convertView.getTag();
        }

        dataBaseItemModel item = list.get(position);
        if (item != null) {
            String[] arrayString = item.getVideoPath().split("/");

            String name = arrayString[arrayString.length-1];

            holder.cellText.setText(name);

            File f = new File(item.getThumb());

            Picasso.get().load(f).into(holder.cellImage);
            holder.cellImage.setScaleType(ImageView.ScaleType.FIT_XY);
        }

        return convertView;

    }


    private static class gridViewHolder {
        public TextView cellText;
        public ImageView cellImage;
    }
}
