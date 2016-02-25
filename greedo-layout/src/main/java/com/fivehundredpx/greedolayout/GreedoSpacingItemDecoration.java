package com.fivehundredpx.greedolayout;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Julian Villella on 15-07-30.
 */
public class GreedoSpacingItemDecoration extends RecyclerView.ItemDecoration {
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
                    GreedoSpacingItemDecoration.class.getSimpleName(), GreedoLayoutManager.class.getSimpleName()));
        }

        int childIndex = parent.getChildAdapterPosition(view);

        GreedoLayoutManager layoutManager = (GreedoLayoutManager) parent.getLayoutManager();
        GreedoLayoutSizeCalculator sizeCalculator = layoutManager.getSizeCalculator();

        outRect.top    = 0;
        outRect.bottom = mSpacing;
        outRect.left   = 0;
        outRect.right  = mSpacing;

        // Add inter-item spacings (don't add spacings on edges)
        if (isTopChild(childIndex, sizeCalculator))
            outRect.top = mSpacing;

        if (isLeftChild(childIndex, sizeCalculator))
            outRect.left = mSpacing;
    }

    private boolean isTopChild(int position, GreedoLayoutSizeCalculator sizeCalculator) {
        return sizeCalculator.getRowForChildPosition(position) == 0;
    }

    private boolean isLeftChild(int position, GreedoLayoutSizeCalculator sizeCalculator) {
        int rowForPosition = sizeCalculator.getRowForChildPosition(position);
        return sizeCalculator.getFirstChildPositionForRow(rowForPosition) == position;
    }
}
