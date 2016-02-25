package com.fivehundredpx.greedolayout;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.fivehundredpx.greedolayout.GreedoLayoutSizeCalculator.SizeCalculatorDelegate;

/**
 * Created by Julian Villella on 15-08-24.
 */

public class GreedoLayoutManager extends RecyclerView.LayoutManager {
    private static final String TAG = GreedoLayoutManager.class.getSimpleName();

    private enum Direction { NONE, UP, DOWN }

    // First (top-left) position visible at any point
    private int mFirstVisiblePosition;

    // First (top) row position at any given point
    private int mFirstVisibleRow;

    // Flag to force current scroll offsets to be ignored on re-layout
    private boolean mForceClearOffsets;

    private GreedoLayoutSizeCalculator mSizeCalculator;

    public GreedoLayoutManager(SizeCalculatorDelegate sizeCalculatorDelegate) {
        mSizeCalculator = new GreedoLayoutSizeCalculator(sizeCalculatorDelegate);
    }

    public void setMaxRowHeight(int maxRowHeight) {
        mSizeCalculator.setMaxRowHeight(maxRowHeight);
    }

    // The initial call from the framework, received when we need to start laying out the initial
    // set of views, or when the user changes the data set
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // We have nothing to show for an empty data set but clear any existing views
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        mSizeCalculator.setContentWidth(getContentWidth());
        mSizeCalculator.reset();

        int childTop;
        if (getChildCount() == 0) { // First or empty layout
            mFirstVisiblePosition = 0;
            mFirstVisibleRow = 0;
            childTop = 0;
        } else { // Adapter data set changes
            // Keep the existing initial position, and save off the current scrolled offset.
            final View topChild = getChildAt(0);
            if (mForceClearOffsets) {
                childTop = 0;
                mForceClearOffsets = false;
            } else {
                childTop = getDecoratedTop(topChild);
            }
        }

        detachAndScrapAttachedViews(recycler);
        preFillGrid(Direction.NONE, 0, childTop, recycler, state);
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
        mSizeCalculator.reset();
    }

    /**
     * Find first visible position, scrap all children, and then layout all visible views returning
     * the number of pixels laid out, which could be greater than the entire view (useful for scroll
     * functions).
     * @param direction The direction we are filling the grid in
     * @param dy Vertical offset, creating a gap that we need to fill
     * @param emptyTop Offset we begin filling at
     * @return Number of vertical pixels laid out
     */
    private int preFillGrid(Direction direction, int dy, int emptyTop,
                            RecyclerView.Recycler recycler, RecyclerView.State state) {
        int newFirstVisiblePosition = mSizeCalculator.getFirstChildPositionForRow(mFirstVisibleRow);

        // First, detach all existing views from the layout. detachView() is a lightweight
        // operation that we can use to quickly reorder views without a full add/remove.
        SparseArray<View> viewCache = new SparseArray<>(getChildCount());
        int startLeftOffset = getPaddingLeft();
        int startTopOffset  = getPaddingTop() + emptyTop;

        if (getChildCount() != 0) {
            startTopOffset = getDecoratedTop(getChildAt(0));

            if (mFirstVisiblePosition != newFirstVisiblePosition) {
                switch (direction) {
                    case UP: // new row above may be shown
                        double previousTopRowHeight = mSizeCalculator.sizeForChildAtPosition(
                                mFirstVisiblePosition - 1).getHeight();
                        startTopOffset -= previousTopRowHeight;
                        break;
                    case DOWN: // row may have gone off screen
                        double topRowHeight = mSizeCalculator.sizeForChildAtPosition(
                                mFirstVisiblePosition).getHeight();
                        startTopOffset += topRowHeight;
                        break;
                }
            }

            // Cache all views by their existing position, before updating counts
            for (int i = 0; i < getChildCount(); i++) {
                int position = mFirstVisiblePosition + i;
                final View child = getChildAt(i);
                viewCache.put(position, child);
            }

            // Temporarily detach all cached views. Views we still need will be added back at the proper index
            for (int i = 0; i < viewCache.size(); i++) {
                final View cachedView = viewCache.valueAt(i);
                detachView(cachedView);
            }
        }

        mFirstVisiblePosition = newFirstVisiblePosition;

        // Next, supply the grid of items that are deemed visible. If they were previously there,
        // they will simply be re-attached. New views that must be created are obtained from the
        // Recycler and added.
        int leftOffset = startLeftOffset;
        int topOffset  = startTopOffset;
        int nextPosition = mFirstVisiblePosition;

        while (nextPosition >= 0 && nextPosition < state.getItemCount()) {
            // Layout this position
            Size viewSize = mSizeCalculator.sizeForChildAtPosition(nextPosition);

            // Overflow to next row if we don't fit
            if ((leftOffset + viewSize.getWidth()) > getContentWidth()) {
                leftOffset = startLeftOffset;
                Size previousViewSize = mSizeCalculator.sizeForChildAtPosition(nextPosition - 1);
                topOffset += previousViewSize.getHeight();
            }

            // These next children would no longer be visible, stop here
            boolean isAtEndOfContent;
            switch (direction) {
                case DOWN: isAtEndOfContent = topOffset >= getContentHeight() + dy; break;
                default:   isAtEndOfContent = topOffset >= getContentHeight();      break;
            }
            if (isAtEndOfContent) break;

            View view = viewCache.get(nextPosition);
            if (view == null) {
                view = recycler.getViewForPosition(nextPosition);
                addView(view);

                measureChildWithMargins(view, 0, 0);

                int right  = leftOffset + viewSize.getWidth();
                int bottom = topOffset  + viewSize.getHeight();
                layoutDecorated(view, leftOffset, topOffset, right, bottom);
            } else {
                // Re-attach the cached view at its new index
                attachView(view);
                viewCache.remove(nextPosition);
            }

            leftOffset += viewSize.getWidth();

            nextPosition++;
        }

        // Scrap and store views that were not re-attached (no longer visible).
        for (int i = 0; i < viewCache.size(); i++) {
            final View removingView = viewCache.valueAt(i);
            recycler.recycleView(removingView);
        }

        // Calculate pixels laid out during fill
        int pixelsFilled = 0;
        if (getChildCount() > 0) {
            pixelsFilled = getChildAt(getChildCount() - 1).getBottom();
        }

        return pixelsFilled;
    }

    private int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    @Override
    public void scrollToPosition(int position) {
        if (position >= getItemCount()) {
            Log.w(TAG, String.format("Cannot scroll to %d, item count is %d", position, getItemCount()));
            return;
        }

        mForceClearOffsets = true; // Ignore current scroll offset, snap to top-left
        mFirstVisibleRow = mSizeCalculator.getRowForChildPosition(position);
        mFirstVisiblePosition = mSizeCalculator.getFirstChildPositionForRow(mFirstVisibleRow);
        requestLayout();
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }

        final View topLeftView = getChildAt(0);
        final View bottomRightView = getChildAt(getChildCount() - 1);
        int pixelsFilled = getContentHeight();
        if (dy > 0) {
            boolean lastChildIsVisible = mFirstVisiblePosition + getChildCount() >= getItemCount();
            if (lastChildIsVisible && pixelsFilled <= getContentHeight()) { // is at end of content
                pixelsFilled = Math.max(getDecoratedBottom(getChildAt(getChildCount() - 1)) - getContentHeight(), 0);
            } else if (getDecoratedBottom(topLeftView) - dy <= 0) { // top row went offscreen
                mFirstVisibleRow++;
                pixelsFilled = preFillGrid(Direction.DOWN, Math.abs(dy), 0, recycler, state);
            } else if (getDecoratedBottom(bottomRightView) - dy < getContentHeight()) { // new bottom row came on screen
                pixelsFilled = preFillGrid(Direction.DOWN, Math.abs(dy), 0, recycler, state);
            }
        } else {
            if (mFirstVisibleRow == 0 && getDecoratedTop(topLeftView) - dy >= 0) { // is scrolled to top
                pixelsFilled = -getDecoratedTop(topLeftView);
            } else if (getDecoratedTop(topLeftView) - dy >= 0) { // new top row came on screen
                mFirstVisibleRow--;
                pixelsFilled = preFillGrid(Direction.UP, Math.abs(dy), 0, recycler, state);
            } else if (getDecoratedTop(bottomRightView) - dy > getContentHeight()) { // bottom row went offscreen
                pixelsFilled = preFillGrid(Direction.UP, Math.abs(dy), 0, recycler, state);
            }
        }

        final int scrolled = Math.abs(dy) > pixelsFilled ? (int)Math.signum(dy) * pixelsFilled : dy;
        offsetChildrenVertical(-scrolled);

        // Return value determines if a boundary has been reached (for edge effects and flings).
        // If returned value does not match original delta (passed in), RecyclerView will draw
        // an edge effect.
        return scrolled;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    public int findFirstVisibleItemPosition() {
        return mFirstVisiblePosition;
    }

    public GreedoLayoutSizeCalculator getSizeCalculator() {
        return mSizeCalculator;
    }
}