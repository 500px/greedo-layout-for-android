package com.fivehundredpx.greedolayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Julian Villella on 15-08-24.
 */

public class GreedoLayoutSizeCalculator {
    public interface SizeCalculatorDelegate {
        double aspectRatioForIndex(int index);
    }

    private static final int DEFAULT_MAX_ROW_HEIGHT = 600;
    private int mMaxRowHeight = DEFAULT_MAX_ROW_HEIGHT;

    private static final int INVALID_CONTENT_WIDTH = -1;
    private int mContentWidth = INVALID_CONTENT_WIDTH;

    // When in fixed height mode and the item's width is less than this percentage, don't try to
    // fit the item, overflow it to the next row and grow the existing items.
    private static final double VALID_ITEM_SLACK_THRESHOLD = 2.0 / 3.0;

    private boolean mIsFixedHeight = false;

    private SizeCalculatorDelegate mSizeCalculatorDelegate;

    private List<Size> mSizeForChildAtPosition;
    private List<Integer> mFirstChildPositionForRow;
    private List<Integer> mRowForChildPosition;

    public GreedoLayoutSizeCalculator(SizeCalculatorDelegate sizeCalculatorDelegate) {
        mSizeCalculatorDelegate = sizeCalculatorDelegate;

        mSizeForChildAtPosition = new ArrayList<>();
        mFirstChildPositionForRow = new ArrayList<>();
        mRowForChildPosition = new ArrayList<>();
    }

    public void setContentWidth(int contentWidth) {
        if (mContentWidth != contentWidth) {
            mContentWidth = contentWidth;
            reset();
        }
    }

    public int getContentWidth() {
        return mContentWidth;
    }

    public void setMaxRowHeight(int maxRowHeight) {
        if (mMaxRowHeight != maxRowHeight) {
            mMaxRowHeight = maxRowHeight;
            reset();
        }
    }

    public void setFixedHeight(boolean fixedHeight) {
        if (mIsFixedHeight != fixedHeight) {
            mIsFixedHeight = fixedHeight;
            reset();
        }
    }

    public Size sizeForChildAtPosition(int position) {
        if (position >= mSizeForChildAtPosition.size()) {
            computeChildSizesUpToPosition(position);
        }

        return mSizeForChildAtPosition.get(position);
    }

    public int getFirstChildPositionForRow(int row) {
        if (row >= mFirstChildPositionForRow.size()) {
            computeFirstChildPositionsUpToRow(row);
        }
        return mFirstChildPositionForRow.get(row);
    }

    public int getRowForChildPosition(int position) {
        if (position >= mRowForChildPosition.size()) {
            computeChildSizesUpToPosition(position);
        }

        return mRowForChildPosition.get(position);
    }

    public void reset() {
        mSizeForChildAtPosition.clear();
        mFirstChildPositionForRow.clear();
        mRowForChildPosition.clear();
    }

    private void computeFirstChildPositionsUpToRow(int row) {
        // TODO: Rewrite this? Looks dangerous but in reality should be fine. I'd like something
        //       less alarming though.
        while (row >= mFirstChildPositionForRow.size()) {
            computeChildSizesUpToPosition(mSizeForChildAtPosition.size() + 1);
        }
    }

    private void computeChildSizesUpToPosition(int lastPosition) {
        if (mContentWidth == INVALID_CONTENT_WIDTH) {
            throw new RuntimeException("Invalid content width. Did you forget to set it?");
        }

        if (mSizeCalculatorDelegate == null) {
            throw new RuntimeException("Size calculator delegate is missing. Did you forget to set it?");
        }

        int firstUncomputedChildPosition = mSizeForChildAtPosition.size();
        int row = mRowForChildPosition.size() > 0
                ? mRowForChildPosition.get(mRowForChildPosition.size() - 1) + 1 : 0;

        double currentRowAspectRatio = 0.0;
        List<Double> itemAspectRatios = new ArrayList<>();
        int currentRowHeight = mIsFixedHeight ? mMaxRowHeight : Integer.MAX_VALUE;

        int currentRowWidth = 0;
        int pos = firstUncomputedChildPosition;
        while (pos <= lastPosition || (mIsFixedHeight ? currentRowWidth <= mContentWidth : currentRowHeight > mMaxRowHeight)) {
            double posAspectRatio = mSizeCalculatorDelegate.aspectRatioForIndex(pos);

            // If the size calculator delegate supplies negative aspect ratio,
            // consider it as "span the entire row" view. It will force a line break
            // and add the view to its own line
            boolean isFullRowView = false;
            if (posAspectRatio < 0) {
                isFullRowView = true;
            } else{
                currentRowAspectRatio += posAspectRatio;
                itemAspectRatios.add(posAspectRatio);
            }

            currentRowWidth = calculateWidth(currentRowHeight, currentRowAspectRatio);
            if (!mIsFixedHeight) {
                currentRowHeight = calculateHeight(mContentWidth, currentRowAspectRatio);
            }

            boolean isRowFull = mIsFixedHeight ? currentRowWidth > mContentWidth : currentRowHeight <= mMaxRowHeight;
            if (isRowFull || isFullRowView) {
                int rowChildCount = itemAspectRatios.size();

                // If the current view is the full row view, the current row is forced to wrap so that
                // the full row view can take the entire row for itself, however the first
                // children on that row needs to be added to mFirstChildPositionForRow as well, otherwise
                // the item decoration will not work
                if (isFullRowView) {
                    mFirstChildPositionForRow.add(pos - rowChildCount);
                }

                mFirstChildPositionForRow.add(pos - rowChildCount + 1);

                int[] itemSlacks = new int[rowChildCount];
                if (mIsFixedHeight) {
                    itemSlacks = distributeRowSlack(currentRowWidth, rowChildCount, itemAspectRatios);

                    if (!hasValidItemSlacks(itemSlacks, itemAspectRatios)) {
                        int lastItemWidth = calculateWidth(currentRowHeight,
                                itemAspectRatios.get(itemAspectRatios.size() - 1));
                        currentRowWidth -= lastItemWidth;
                        rowChildCount -= 1;
                        itemAspectRatios.remove(itemAspectRatios.size() - 1);

                        itemSlacks = distributeRowSlack(currentRowWidth, rowChildCount, itemAspectRatios);
                    }
                }

                int availableSpace = mContentWidth;
                for (int i = 0; i < rowChildCount; i++) {
                    // If the previous row was force-wrapped and there was a single photo, the row
                    // size would be computed from that single photo - this could make the row huge
                    // because the aspect ratio of that single photo would be used. So this limits
                    // it to something reasonable
                    if (isFullRowView && !isRowFull) {
                        currentRowHeight = (int) Math.ceil(mMaxRowHeight * 0.75);
                    }
                    int itemWidth = calculateWidth(currentRowHeight, itemAspectRatios.get(i)) - itemSlacks[i];
                    itemWidth = Math.min(availableSpace, itemWidth);

                    mSizeForChildAtPosition.add(new Size(itemWidth, currentRowHeight));
                    mRowForChildPosition.add(row);

                    availableSpace -= itemWidth;
                }

                // Now add a size for the full row view
                if (isFullRowView) {
                    mSizeForChildAtPosition.add(new Size(mContentWidth, calculateHeight(mContentWidth, Math.abs(posAspectRatio))));
                    mRowForChildPosition.add(row++);
                }

                itemAspectRatios.clear();
                currentRowAspectRatio = 0.0;
                row++;
            }

            pos++;
        }
    }

    private int[] distributeRowSlack(int rowWidth, int rowChildCount, List<Double> itemAspectRatios) {
        return distributeRowSlack(rowWidth - mContentWidth, rowWidth, rowChildCount, itemAspectRatios);
    }

    private int[] distributeRowSlack(int rowSlack, int rowWidth, int rowChildCount, List<Double> itemAspectRatios) {
        int itemSlacks[] = new int[rowChildCount];

        for (int i = 0; i < rowChildCount; i++) {
            double itemWidth = mMaxRowHeight * itemAspectRatios.get(i);
            itemSlacks[i] = (int) (rowSlack * (itemWidth / rowWidth));
        }

        return itemSlacks;
    }

    private boolean hasValidItemSlacks(int[] itemSlacks, List<Double> itemAspectRatios) {
        for (int i = 0; i < itemSlacks.length; i++) {
            int itemWidth = (int) (itemAspectRatios.get(i) * mMaxRowHeight);
            if (!isValidItemSlack(itemSlacks[i], itemWidth)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidItemSlack(int itemSlack, int itemWidth) {
        return (itemWidth - itemSlack) / (double) itemWidth > VALID_ITEM_SLACK_THRESHOLD;
    }

    private int calculateWidth(int itemHeight, double aspectRatio) {
        return (int) Math.ceil(itemHeight * aspectRatio);
    }

    private int calculateHeight(int itemWidth, double aspectRatio) {
        return (int) Math.ceil(itemWidth / aspectRatio);
    }
}
