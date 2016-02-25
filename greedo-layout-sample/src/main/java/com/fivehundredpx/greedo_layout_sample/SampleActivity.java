package com.fivehundredpx.greedo_layout_sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;

import com.fivehundredpx.greedolayout.AspectRatioLayoutManager;
import com.fivehundredpx.greedolayout.AspectRatioSpacingItemDecoration;

/**
 * Created by Julian Villella on 16-02-24.
 */
public class SampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        PhotosAdapter photosAdapter = new PhotosAdapter(this);
        AspectRatioLayoutManager layoutManager = new AspectRatioLayoutManager(photosAdapter);

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(photosAdapter);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int maxRowHeight = metrics.heightPixels / 3;
        layoutManager.setMaxRowHeight(maxRowHeight);

        int spacing = MeasUtils.dpToPx(4, this);
        recyclerView.addItemDecoration(new AspectRatioSpacingItemDecoration(spacing));
    }
}
