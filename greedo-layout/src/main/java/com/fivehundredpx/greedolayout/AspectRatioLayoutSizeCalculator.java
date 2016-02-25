package com.fivehundredpx.greedolayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Julian Villella on 15-08-24.
 */

public class AspectRatioLayoutSizeCalculator {
    public interface SizeCalculatorDelegate {
        double aspectRatioForIndex(int index);
    }

    private static final int DEFAULT_MAX_ROW_HEIGHT = 600;
    private static int mMaxRowHeight = DEFAULT_MAX_ROW_HEIGHT;

    private static final int INVALID_CONTENT_WIDTH = -1;
    private int mContentWidth = INVALID_CONTENT_WIDTH;

    private SizeCalculatorDelegate mSizeCalculatorDelegate;

    private List<Size> mSizeForChildAtPosition;
    private List<Integer> mFirstChildPositionForRow;
    private List<Integer> mRowForChildPosition;

    public AspectRatioLayoutSizeCalculator(SizeCalculatorDelegate sizeCalculatorDelegate) {
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

    public void setMaxRowHeight(int maxRowHeight) {
        if (mMaxRowHeight != maxRowHeight) {
            mMaxRowHeight = maxRowHeight;
            reset();
        }
    }

    Size sizeForChildAtPosition(int position) {
        if (position >= mSizeForChildAtPosition.size()) {
            computeChildSizesUpToPosition(position);
        }

        return mSizeForChildAtPosition.get(position);
    }

    int getFirstChildPositionForRow(int row) {
        if (row >= mFirstChildPositionForRow.size()) {
            computeFirstChildPositionsUpToRow(row);
        }
        return mFirstChildPositionForRow.get(row);
    }

    private void computeFirstChildPositionsUpToRow(int row) {
        // TODO: Rewrite this? Looks dangerous but in reality should be fine. I'd like something
        // less alarming though.
        while (row >= mFirstChildPositionForRow.size()) {
            computeChildSizesUpToPosition(mSizeForChildAtPosition.size() + 1);
        }
    }

    int getRowForChildPosition(int position) {
        if (position >= mRowForChildPosition.size()) {
            computeChildSizesUpToPosition(position);
        }

        return mRowForChildPosition.get(position);
    }

    void reset() {
        mSizeForChildAtPosition.clear();
        mFirstChildPositionForRow.clear();
        mRowForChildPosition.clear();
    }

    public void computeChildSizesUpToPosition(int lastPosition) {
        if (mContentWidth == INVALID_CONTENT_WIDTH) {
            throw new RuntimeException("Invalid content width. Did you forget to set it?");
        }

        if (mSizeCalculatorDelegate == null) {
            throw new RuntimeException("Size calculator delegate is missing. Did you forget to set it?");
        }

        int firstUncomputedChildPosition = mSizeForChildAtPosition.size();
        int row = mRowForChildPosition.size() > 0
                ? mRowForChildPosition.get(mRowForChildPosition.size() - 1) + 1 : 0;

        double rowAspectRatio = 0.0;
        int currentRowHeight = Integer.MAX_VALUE;
        List<Double> aspectRatiosForRow = new ArrayList<>();
        for (int pos = firstUncomputedChildPosition; pos < lastPosition || currentRowHeight > mMaxRowHeight; pos++) {
            double posAspectRatio = mSizeCalculatorDelegate.aspectRatioForIndex(pos);
            rowAspectRatio += posAspectRatio;
            aspectRatiosForRow.add(posAspectRatio);

            currentRowHeight = (int)Math.ceil(mContentWidth / rowAspectRatio);
            if (currentRowHeight <= mMaxRowHeight) { // Row is full
                int rowChildCount = aspectRatiosForRow.size();
                mFirstChildPositionForRow.add(pos - rowChildCount + 1);

                int availableSpace = mContentWidth;
                for (Double aspectRatio : aspectRatiosForRow) {
                    int scaledPhotoWidth = (int)Math.ceil(currentRowHeight * aspectRatio);
                    scaledPhotoWidth = Math.min(availableSpace, scaledPhotoWidth);

                    mSizeForChildAtPosition.add(new Size(scaledPhotoWidth, currentRowHeight));
                    mRowForChildPosition.add(row);

                    availableSpace = availableSpace - scaledPhotoWidth;
                }

                aspectRatiosForRow.clear();
                rowAspectRatio = 0.0;
                row++;
            }
        }
    }
}
