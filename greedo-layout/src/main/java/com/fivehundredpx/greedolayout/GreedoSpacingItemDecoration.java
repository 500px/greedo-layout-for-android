package com.fivehundredpx.greedolayout;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Julian Villella on 15-07-30.
 */
public class GreedoSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private static final String TAG = GreedoSpacingItemDecoration.class.getName();

    public static int DEFAULT_SPACING = 64;
    private int mSpacing;

    public GreedoSpacingItemDecoration() {
        this(DEFAULT_SPACING);
    }

    public GreedoSpacingItemDecoration(int spacing) {
        mSpacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (!(parent.getLayoutManager() instanceof GreedoLayoutManager)) {
            throw new IllegalArgumentException(String.format("The %s must be used with a %s",
                    GreedoSpacingItemDecoration.class.getSimpleName(),
                    GreedoLayoutManager.class.getSimpleName()));
        }

        final GreedoLayoutManager layoutManager = (GreedoLayoutManager) parent.getLayoutManager();

        int childIndex = parent.getChildAdapterPosition(view);
        if (childIndex == RecyclerView.NO_POSITION) return;

        outRect.top    = 0;
        outRect.bottom = mSpacing;
        outRect.left   = 0;
        outRect.right  = mSpacing;

        // Add inter-item spacings
        if (isTopChild(childIndex, layoutManager)) {
            outRect.top = mSpacing;
        }

        if (isLeftChild(childIndex, layoutManager)) {
            outRect.left = mSpacing;
        }
    }

    private static boolean isTopChild(int position, GreedoLayoutManager layoutManager) {
        boolean isFirstViewHeader = layoutManager.isFirstViewHeader();
        if (isFirstViewHeader && position == GreedoLayoutManager.HEADER_POSITION) {
            return true;
        } else if (isFirstViewHeader && position > GreedoLayoutManager.HEADER_POSITION) {
            // Decrement position to factor in existence of header
            position -= 1;
        }

        final GreedoLayoutSizeCalculator sizeCalculator = layoutManager.getSizeCalculator();
        return sizeCalculator.getRowForChildPosition(position) == 0;
    }

    private static boolean isLeftChild(int position, GreedoLayoutManager layoutManager) {
        boolean isFirstViewHeader = layoutManager.isFirstViewHeader();
        if (isFirstViewHeader && position == GreedoLayoutManager.HEADER_POSITION) {
            return true;
        } else if (isFirstViewHeader && position > GreedoLayoutManager.HEADER_POSITION) {
            // Decrement position to factor in existence of header
            position -= 1;
        }

        final GreedoLayoutSizeCalculator sizeCalculator = layoutManager.getSizeCalculator();
        int rowForPosition = sizeCalculator.getRowForChildPosition(position);
        return sizeCalculator.getFirstChildPositionForRow(rowForPosition) == position;
    }
}
