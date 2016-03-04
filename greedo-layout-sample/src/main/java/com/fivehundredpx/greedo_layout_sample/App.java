package com.fivehundredpx.greedo_layout_sample;

import android.app.ActivityManager;
import android.app.Application;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

/**
 * Created by Julian Villella on 16-02-24.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Picasso picasso = new Picasso.Builder(this)
                .memoryCache(new LruCache(calculateMemoryCacheSize()))
                .build();
        Picasso.setSingletonInstance(picasso);
    }

    private int calculateMemoryCacheSize() {
        ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        boolean largeHeap = (getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && SDK_INT >= HONEYCOMB) {
            memoryClass = am.getLargeMemoryClass();
        }

        // Target ~50% of the available heap.
        return 1024 * 1024 * memoryClass / 2;
    }
}
