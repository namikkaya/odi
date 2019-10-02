package com.odi.beranet.beraodi.odiLib.videoGalleryLibrary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.odi.beranet.beraodi.R;
import com.odi.beranet.beraodi.models.dataBaseItemModel;

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
            holder.cellIdText = convertView.findViewById(R.id.cellId);
            convertView.setTag(holder);
        }else {
            holder = (gridViewHolder) convertView.getTag();
        }

        dataBaseItemModel item = list.get(position);
        if (item != null) {
            holder.cellText.setText(item.getVideoPath());
            holder.cellIdText.setText(item.getId());
        }

        return convertView;

    }

    private static class gridViewHolder {
        public TextView cellText;
        public TextView cellIdText;
    }
}
