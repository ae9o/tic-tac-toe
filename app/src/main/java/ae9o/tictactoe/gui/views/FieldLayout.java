/*
 * Copyright (C) 2022 Alexei Evdokimenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ae9o.tictactoe.gui.views;

import ae9o.tictactoe.core.TicTacToeGame;
import ae9o.tictactoe.gui.utils.Quantity;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FieldLayout extends TableLayout {
    private static final int DEFAULT_MARGIN = 10;
    private static final int DEFAULT_STROKE_WIDTH = DEFAULT_MARGIN * 2;

    private static final Rect tmpRect = new Rect();

    private Paint paint;
    private float backgroundAnimationProgress;
    private float comboAnimationProgress;
    private int backgroundColor = Color.WHITE;
    private int foregroundColor = Color.LTGRAY;
    private TableLayout.LayoutParams rowLayoutParams;
    private TableRow.LayoutParams cellLayoutParams;
    private int xForegroundColor = Color.BLACK;
    private int oForegroundColor = Color.BLACK;

    private List<TableRow> rowPool;
    private List<FieldCell> cellPool;

    private FieldCell[][] cells;
    private int size;

    private float startComboX;
    private float startComboY;
    private float comboDeltaX;
    private float comboDeltaY;

    private OnCellClickListener onCellClickListener;

    public FieldLayout(Context context) {
        super(context);
        init(context, null);
    }

    public FieldLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setStretchAllColumns(true);
        setWillNotDraw(false);

        paint = new Paint();
        paint.setStrokeWidth(DEFAULT_STROKE_WIDTH);

        rowLayoutParams = new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        rowLayoutParams.setMargins(DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, 0);

        cellLayoutParams = new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        cellLayoutParams.setMargins(0, 0, DEFAULT_MARGIN, 0);

        rowPool = new ArrayList<>();
        cellPool = new ArrayList<>();
    }

    @Nullable
    public OnCellClickListener getOnCellClickListener() {
        return onCellClickListener;
    }

    public void setOnCellClickListener(@Nullable OnCellClickListener onCellClickListener) {
        this.onCellClickListener = onCellClickListener;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        if (backgroundColor == this.backgroundColor) {
            return;
        }
        this.backgroundColor = backgroundColor;

        invalidate();
    }

    public int getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(int foregroundColor) {
        if (foregroundColor == this.foregroundColor) {
            return;
        }
        this.foregroundColor = foregroundColor;

        invalidate();
    }

    public int getSize() {
        return size;
    }

    private void setSize(int size) {
        if (size == this.size) {
            return;
        }
        this.size = size;

        if ((cells != null) && (cells.length >= size)) {
            return;
        }
        cells = new FieldCell[size][];
        for (int i = 0; i < size; ++i) {
            cells[i] = new FieldCell[size];
        }
    }

    public FieldCell getCell(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IllegalArgumentException("Cell location out of range");
        }
        return cells[row][col];
    }

    public void compose(int size) {
        decompose();

        final Context context = getContext();
        setSize(size);
        for (int i = 0; i < size; ++i) {
            final TableRow row = obtainTableRow(context);
            row.setLayoutParams(rowLayoutParams);
            for (int j = 0; j < size; ++j) {
                final FieldCell cell = obtainMarkView(context);
                cell.setLayoutParams(cellLayoutParams);
                cell.setFieldLayout(this);
                cell.setLocation(i, j);
                cell.setMark(TicTacToeGame.Mark.EMPTY);
                cell.setClickable(true);
                cell.setXForegroundColor(xForegroundColor);
                cell.setOForegroundColor(oForegroundColor);
                row.addView(cell);
                cells[i][j] = cell;
            }
            addView(row);
        }

        startBackgroundAnimation();
    }

    public void decompose() {
        freeRowsAndCells();
        clearCells();
        clearCombo();
    }

    public int getXForegroundColor() {
        return xForegroundColor;
    }

    public void setXForegroundColor(int xForegroundColor) {
        this.xForegroundColor = xForegroundColor;
    }

    public int getOForegroundColor() {
        return oForegroundColor;
    }

    public void setOForegroundColor(int oForegroundColor) {
        this.oForegroundColor = oForegroundColor;
    }

    public void setCombo(TicTacToeGame.Combo combo) {
        final View start = cells[combo.getStartRow()][combo.getStartCol()];
        start.getDrawingRect(tmpRect);
        offsetDescendantRectToMyCoords(start, tmpRect);
        startComboX = tmpRect.centerX();
        startComboY = tmpRect.centerY();

        final View end = cells[combo.getStopRow()][combo.getStopCol()];
        end.getDrawingRect(tmpRect);
        offsetDescendantRectToMyCoords(end, tmpRect);

        comboDeltaX = tmpRect.centerX() - startComboX;
        comboDeltaY = tmpRect.centerY() - startComboY;

        startComboAnimation();
    }

    public void clearCombo() {
        startComboX = 0;
        startComboY = 0;
        comboDeltaX = 0;
        comboDeltaY = 0;
    }

    public boolean hasCombo() {
        return (comboDeltaX != 0) || (comboDeltaY != 0);
    }

    private void clearCells() {
        for (int i = 0; i < size; ++i) {
            Arrays.fill(cells[i], null);
        }
    }

    private TableRow obtainTableRow(Context context) {
        if (rowPool.isEmpty()) {
            return new TableRow(context);
        }
        return rowPool.remove(rowPool.size() - 1);
    }

    private FieldCell obtainMarkView(Context context) {
        if (cellPool.isEmpty()) {
            return new FieldCell(context);
        }
        return cellPool.remove(cellPool.size() - 1);
    }

    private void freeRowsAndCells() {
        for (int i = 0, n = getChildCount(); i < n; ++i) {
            final View group = getChildAt(i);
            if (group instanceof TableRow) {
                final TableRow row = (TableRow) group;
                rowPool.add(row);
                for (int j = 0, m = row.getChildCount(); j < m; ++j) {
                    final View view = row.getChildAt(j);
                    if (view instanceof FieldCell) {
                        cellPool.add((FieldCell) view);
                    }
                }
                row.removeAllViews();
            }
        }
        removeAllViews();
    }

    private final ValueAnimator.AnimatorUpdateListener backgroundAnimationUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            backgroundAnimationProgress = (float) animation.getAnimatedValue();
            invalidate();
        }
    };

    private void startBackgroundAnimation() {
        backgroundAnimationProgress = Quantity.NONE;

        ValueAnimator animator = ValueAnimator.ofFloat(Quantity.NONE, Quantity.FULL);
        animator.addUpdateListener(backgroundAnimationUpdateListener);
        animator.start();
    }

    private final ValueAnimator.AnimatorUpdateListener comboAnimationUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            comboAnimationProgress = (float) animation.getAnimatedValue();
            invalidate();
        }
    };

    private void startComboAnimation() {
        comboAnimationProgress = Quantity.NONE;

        ValueAnimator animator = ValueAnimator.ofFloat(Quantity.NONE, Quantity.FULL);
        animator.addUpdateListener(comboAnimationUpdateListener);
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float width = getWidth();
        final float height = getHeight();

        // Clear the canvas.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(backgroundColor);
        canvas.drawRect(0, 0, width, height, paint);

        // Draw a rectangle growing from the center of the game field.
        final float halfWidth = width * Quantity.HALF;
        final float halfHeight = height * Quantity.HALF;
        final float x = halfWidth - halfWidth * backgroundAnimationProgress;
        final float y = halfHeight - halfHeight * backgroundAnimationProgress;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(foregroundColor);
        canvas.drawRect(x, y, width - x, height - y, paint);

        // Remove the frame around the game field.
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(backgroundColor);
        canvas.drawRect(0, 0, width, height + DEFAULT_MARGIN, paint);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawCombo(canvas);
    }

    private void drawCombo(Canvas canvas) {
        if (hasCombo()) {
            paint.setColor(Color.RED);
            canvas.drawLine(startComboX, startComboY, startComboX + comboDeltaX * comboAnimationProgress,
                    startComboY + comboDeltaY * comboAnimationProgress, paint);
        }
    }

    public void notifyCellClicked(FieldCell cell) {
        if (onCellClickListener != null) {
            onCellClickListener.onCellClick(cell, cell.getRow(), cell.getCol());
        }
    }

    public interface OnCellClickListener {
        void onCellClick(FieldCell cell, int row, int col);
    }
}
