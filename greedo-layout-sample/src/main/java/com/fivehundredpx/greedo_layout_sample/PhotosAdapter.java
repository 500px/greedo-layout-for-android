package com.fivehundredpx.greedo_layout_sample;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.fivehundredpx.greedolayout.AspectRatioLayoutSizeCalculator.SizeCalculatorDelegate;
import com.squareup.picasso.Picasso;

/**
 * Created by Julian Villella on 16-02-24.
 */
public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder> implements SizeCalculatorDelegate {
    private final @DrawableRes int[] mImageResIds = new int[Constants.IMAGE_COUNT];

    private Context mContext;

    @Override
    public double aspectRatioForIndex(int index) {
        // Precaution, have better handling for this in greedo-layout
        if (index >= mImageResIds.length) return 1.0;

        Drawable drawable = mContext.getResources().getDrawable(mImageResIds[index]);
        if (drawable != null) {
            return drawable.getIntrinsicWidth() / (double) drawable.getIntrinsicHeight();
        } else {
            return 1.0;
        }
    }

    public class PhotoViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;
        public PhotoViewHolder(ImageView imageView) {
            super(imageView);
            mImageView = imageView;
        }
    }

    public PhotosAdapter(Context context) {
        mContext = context;

        // Show random sampling of images
        for (int i = 0; i < Constants.IMAGE_COUNT; i++) {
            int idx = i % Constants.IMAGES.length;
            mImageResIds[i] = Constants.IMAGES[idx];
        }
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        ImageView imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return new PhotoViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        Context context = holder.mImageView.getContext();
        Picasso.with(context)
                .load(mImageResIds[position])
                .into(holder.mImageView);
    }

    @Override
    public int getItemCount() {
        return mImageResIds.length;
    }
}
