package com.odi.beranet.beraodi.Activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.odi.beranet.beraodi.R;
import com.odi.beranet.beraodi.models.Model_images;

import java.util.ArrayList;

public class Adapter_PhotosFolder extends ArrayAdapter {
    String TAG = "Adapter_PhotosFolder";

    private final Context context;
    private ViewHolder viewHolder;


    ArrayList<Model_images> al_menu = new ArrayList<>();

    private final LayoutInflater inflater;


    public Adapter_PhotosFolder(Context context, ArrayList<Model_images> al_menu) {
        super(context, R.layout.gallery_gridview_cell, al_menu);
        this.al_menu = al_menu;
        this.context = context;
        this.inflater = LayoutInflater.from(context);

        System.out.println(TAG + " Adapter_PhotosFolder ---- image");
    }

    @Override
    public int getCount() {
        return al_menu.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        if (al_menu.size() > 0) {
            return al_menu.size();
        } else {
            return super.getViewTypeCount();
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        //System.out.println(KAYATAG + " getView ---- image");

        if (convertView == null) {

            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.gallery_gridview_cell, null);

            //convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_photosfolder, parent, false);

            viewHolder.iv_image = (ImageView) convertView.findViewById(R.id.iv_image);
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }




        Glide.with(context).load("file://" + al_menu.get(position).getAl_imagepath().get(0))
                /*.diskCacheStrategy(DiskCacheStrategy.ALL)*/
                /*.skipMemoryCache(true)*/
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(viewHolder.iv_image);


        return convertView;
    }

    private static class ViewHolder {
        ImageView iv_image;
    }


}
