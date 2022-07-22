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

/**
 * {@link TableLayout} which implements a field of {@link FieldCell} ordered in rows and columns.
 *
 * <p>Allows to subscribe to cell click events.
 *
 * <p>Supports animated display of game events (for example, the completion of a combo).
 */
public class FieldLayout extends TableLayout {
    /** Default margin for cells. */
    private static final int DEFAULT_MARGIN = 10;
    /** Default stroke width for different animations. */
    private static final int DEFAULT_STROKE_WIDTH = 20;

    /** Lots of stuff to draw animations. */
    private Paint paint;
    private float backgroundAnimationProgress;
    private float comboAnimationProgress;
    private int backgroundColor = Color.WHITE;
    private int foregroundColor = Color.LTGRAY;
    private TableLayout.LayoutParams rowLayoutParams;
    private TableRow.LayoutParams cellLayoutParams;

    /** Reusable building material for new fields. */
    private List<TableRow> rowPool;
    private List<FieldCell> cellPool;

    /** The current state of the field. */
    private FieldCell[][] cells;
    private int size;

    /** Coordinates of the combo. */
    private float startComboX;
    private float startComboY;
    private float comboDeltaX;
    private float comboDeltaY;

    /** Cell click listener. */
    private OnCellClickListener onCellClickListener;

    private static final Rect tmpRect = new Rect();

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     */
    public FieldLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called
     * when a view is being constructed from an XML file, supplying attributes
     * that were specified in the XML file. This version uses a default style of
     * 0, so the only attribute values applied are those in the Context's Theme
     * and the given AttributeSet.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public FieldLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

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

    /**
     * Returns the active cell click listener.
     */
    @Nullable
    public OnCellClickListener getOnCellClickListener() {
        return onCellClickListener;
    }

    /**
     * Sets this view's new cell click listener.
     *
     * <p>This view supports only one active listener. The new listener replaces the old one.
     *
     * @param onCellClickListener The new listener.
     */
    public void setOnCellClickListener(@Nullable OnCellClickListener onCellClickListener) {
        this.onCellClickListener = onCellClickListener;
    }

    /**
     * Returns this view's background color.
     */
    public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets this view's new background color.
     *
     * @param backgroundColor The new color of the background.
     */
    @Override
    public void setBackgroundColor(int backgroundColor) {
        if (backgroundColor == this.backgroundColor) {
            return;
        }
        this.backgroundColor = backgroundColor;

        invalidate();
    }

    /**
     * Returns this view's foreground color.
     */
    public int getForegroundColor() {
        return foregroundColor;
    }

    /**
     * Sets this view's new foreground color.
     *
     * @param foregroundColor The new color of the foreground.
     */
    public void setForegroundColor(int foregroundColor) {
        if (foregroundColor == this.foregroundColor) {
            return;
        }
        this.foregroundColor = foregroundColor;

        invalidate();
    }

    /**
     * Returns the actual size of the field.
     */
    public int getSize() {
        return size;
    }

    /**
     * For internal use.
     *
     * <p>Prepares an array to hold a field of the given size.
     *
     * <p>If the capacity of the current array is not less than the required size, then it will be reused.
     * If the current array has a smaller size, then a new one of the required size will be created.
     *
     * @param size The required size of the new field.
     */
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

    /**
     * Returns the field cell with the given coordinates.
     *
     * @param row The row coordinate.
     * @param col The col coordinate.
     * @return Cell with given coordinates.
     */
    public FieldCell getCell(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IllegalArgumentException("Cell location out of range");
        }
        return cells[row][col];
    }

    /**
     * Composes a new field of the specified size.
     *
     * <p>If a field has already been composed before, it will be disassembled into parts that will be reused.
     *
     * @param size The required size of the new field.
     */
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
                row.addView(cell);
                cells[i][j] = cell;
            }
            addView(row);
        }

        startBackgroundAnimation();
    }

    /**
     * Disassembles current field into parts that will be reused later.
     */
    public void decompose() {
        freeRowsAndCells();
        clearCells();
        clearCombo();
        setSize(0);
    }

    /**
     * Sets coordinates of the new combo.
     *
     * @param combo The new combo.
     */
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

    /**
     * Clears coordinates of the combo.
     */
    public void clearCombo() {
        startComboX = 0;
        startComboY = 0;
        comboDeltaX = 0;
        comboDeltaY = 0;
    }

    /**
     * Returns true if combo is active.
     */
    public boolean hasCombo() {
        return (comboDeltaX != 0) || (comboDeltaY != 0);
    }

    /**
     * For internal use.
     *
     * <p>Fills the field with nulls.
     */
    private void clearCells() {
        for (int i = 0; i < size; ++i) {
            Arrays.fill(cells[i], null);
        }
    }

    /**
     * For internal use.
     *
     * <p>Attempts to retrieve a table row for reuse. If the pool runs out, creates a new one.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @return A new {@link TableRow}.
     */
    private TableRow obtainTableRow(Context context) {
        if (rowPool.isEmpty()) {
            return new TableRow(context);
        }
        return rowPool.remove(rowPool.size() - 1);
    }

    /**
     * For internal use.
     *
     * <p>Attempts to retrieve a field cell for reuse. If the pool runs out, creates a new one.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @return A new {@link FieldCell}.
     */
    private FieldCell obtainMarkView(Context context) {
        if (cellPool.isEmpty()) {
            return new FieldCell(context);
        }
        return cellPool.remove(cellPool.size() - 1);
    }

    /**
     * For internal use.
     *
     * <p>Returns all used rows and cell from the field to their pools for reuse.
     */
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

    /**
     * Animates the background.
     */
    private final ValueAnimator.AnimatorUpdateListener backgroundAnimationUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            backgroundAnimationProgress = (float) animation.getAnimatedValue();
            invalidate();
        }
    };

    /**
     * Starts the background animation from the beginning.
     */
    private void startBackgroundAnimation() {
        backgroundAnimationProgress = Quantity.NONE;

        ValueAnimator animator = ValueAnimator.ofFloat(Quantity.NONE, Quantity.FULL);
        animator.addUpdateListener(backgroundAnimationUpdateListener);
        animator.start();
    }

    /**
     * Animates the appearance of the combo.
     */
    private final ValueAnimator.AnimatorUpdateListener comboAnimationUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            comboAnimationProgress = (float) animation.getAnimatedValue();
            invalidate();
        }
    };

    /**
     * Starts the combo animation from the beginning.
     */
    private void startComboAnimation() {
        comboAnimationProgress = Quantity.NONE;

        ValueAnimator animator = ValueAnimator.ofFloat(Quantity.NONE, Quantity.FULL);
        animator.addUpdateListener(comboAnimationUpdateListener);
        animator.start();
    }

    /**
     * Draws an animation of a grid growing from the center of the field.
     *
     * @param canvas The canvas on which the background will be drawn.
     */
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

    /**
     * Draws a combo over the main content of the field.
     *
     * @param canvas The canvas on which to draw the view.
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawCombo(canvas);
    }

    /**
     * Draws an animated line that gradually crosses out the combo.
     *
     * @param canvas The canvas on which to draw the view.
     */
    private void drawCombo(Canvas canvas) {
        if (hasCombo()) {
            paint.setColor(Color.RED);
            canvas.drawLine(startComboX, startComboY, startComboX + comboDeltaX * comboAnimationProgress,
                    startComboY + comboDeltaY * comboAnimationProgress, paint);
        }
    }

    /**
     * Notifies the listener that a cell of the field has been clicked.
     *
     * <p>Intended to be called by field cells.
     *
     * @param cell The cell that was clicked.
     */
    public void notifyCellClicked(FieldCell cell) {
        if (onCellClickListener != null) {
            onCellClickListener.onCellClick(cell, cell.getRow(), cell.getCol());
        }
    }

    /**
     * Cell click listener.
     */
    public interface OnCellClickListener {
        void onCellClick(FieldCell cell, int row, int col);
    }
}
