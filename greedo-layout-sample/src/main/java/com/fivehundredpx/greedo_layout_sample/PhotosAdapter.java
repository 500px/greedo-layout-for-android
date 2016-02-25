package com.fivehundredpx.greedo_layout_sample;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by JVillella on 16-02-24.
 */
public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder> {
    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    private static class PhotoViewHolder extends RecyclerView.ViewHolder {
        public PhotoViewHolder(View itemView) {
            super(itemView);
        }
    }
}
