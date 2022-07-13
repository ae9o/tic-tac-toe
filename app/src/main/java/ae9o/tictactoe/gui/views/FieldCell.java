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

import ae9o.tictactoe.R;
import ae9o.tictactoe.core.TicTacToeGame.Mark;
import ae9o.tictactoe.gui.utils.Quantity;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

public class FieldCell extends View {
    private static final int DEFAULT_STROKE_WIDTH = 20;

    private FieldLayout fieldLayout;
    private Mark mark = Mark.EMPTY;
    private int row;
    private int col;

    private Paint backgroundPaint;
    private Paint foregroundPaint;
    private int activeBackgroundColor = Color.LTGRAY;
    private int inactiveBackgroundColor = Color.WHITE;
    private int xForegroundColor = Color.BLACK;
    private int oForegroundColor = Color.BLACK;
    private float animationProgress;
    private CellDrawable drawable = EMPTY_DRAWABLE;

    public FieldCell(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public FieldCell(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context,  attrs, 0, 0);
    }

    public FieldCell(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    public FieldCell(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(inactiveBackgroundColor);

        foregroundPaint = new Paint();
        foregroundPaint.setStyle(Paint.Style.STROKE);
        foregroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        foregroundPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);

        parseAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private void parseAttributes(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) {
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FieldCell, defStyleAttr, defStyleRes);
        try {
            xForegroundColor = a.getColor(R.styleable.FieldCell_xForegroundColor, 0);
            oForegroundColor = a.getColor(R.styleable.FieldCell_oForegroundColor, 0);

            switch (a.getInteger(R.styleable.FieldCell_mark, 0)) {
                case Mark.X_ORDINAL:
                    setMark(Mark.X);
                    break;

                case Mark.O_ORDINAL:
                    setMark(Mark.O);
                    break;
            }
        } finally {
            a.recycle();
        }
    }

    @Nullable
    public FieldLayout getFieldLayout() {
        return fieldLayout;
    }

    public void setFieldLayout(@Nullable FieldLayout fieldLayout) {
        this.fieldLayout = fieldLayout;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setLocation(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getForegroundColor() {
        return foregroundPaint.getColor();
    }

    public void setForegroundColor(int foregroundColor) {
        if (foregroundColor == foregroundPaint.getColor()) {
            return;
        }
        foregroundPaint.setColor(foregroundColor);

        invalidate();
    }

    public int getActiveBackgroundColor() {
        return activeBackgroundColor;
    }

    public void setActiveBackgroundColor(int activeBackgroundColor) {
        this.activeBackgroundColor = activeBackgroundColor;
    }

    public int getInactiveBackgroundColor() {
        return inactiveBackgroundColor;
    }

    public void setInactiveBackgroundColor(int inactiveBackgroundColor) {
        this.inactiveBackgroundColor = inactiveBackgroundColor;
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

    public Mark getMark() {
        return mark;
    }

    public void setMark(Mark mark) {
        if (mark == null) {
            throw new IllegalArgumentException("Mark parameter cannot be null");
        }
        if (mark == this.mark) {
            return;
        }
        this.mark = mark;

        onMarkChanged();
    }

    protected void onMarkChanged() {
        switch (mark) {
            case X:
                drawable = X_DRAWABLE;
                break;

            case O:
                drawable = O_DRAWABLE;
                break;

            default:
                drawable = EMPTY_DRAWABLE;
        }

        if (drawable.isAnimated()) {
            startAnimation();
        }
    }

    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        }
    };

    public void startAnimation() {
        animationProgress = Quantity.NONE;

        ValueAnimator animator = ValueAnimator.ofFloat(Quantity.NONE, Quantity.FULL);
        animator.addUpdateListener(animatorUpdateListener);
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int minWidth = getMinimumWidth();

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int width;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(minWidth, widthSize);
        } else {
            width = minWidth;
        }

        //noinspection SuspiciousNameCombination
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawable.draw(this, canvas);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isClickable()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    backgroundPaint.setColor(activeBackgroundColor);
                    invalidate();
                    break;

                case MotionEvent.ACTION_UP:
                    backgroundPaint.setColor(inactiveBackgroundColor);
                    invalidate();
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        notifyFieldLayoutCellClicked();
        invalidate();
        return super.performClick();
    }

    protected void notifyFieldLayoutCellClicked() {
        if (fieldLayout != null) {
            fieldLayout.notifyCellClicked(this);
        }
    }

    protected static class CellDrawable {
        public boolean isAnimated() {
            return false;
        }

        public void draw(FieldCell cell, Canvas canvas) {
            canvas.drawRect(0, 0, cell.getWidth(), cell.getHeight(), cell.backgroundPaint);
        }
    }

    protected static final CellDrawable EMPTY_DRAWABLE = new CellDrawable();

    protected static final CellDrawable X_DRAWABLE = new CellDrawable() {
        @Override
        public boolean isAnimated() {
            return true;
        }

        @Override
        public void draw(FieldCell cell, Canvas canvas) {
            super.draw(cell, canvas);

            final Paint paint = cell.foregroundPaint;
            paint.setColor(cell.xForegroundColor);

            final float width = cell.getWidth();
            final float height = cell.getHeight();
            final float halfWidth = width * Quantity.HALF;
            final float halfHeight = height * Quantity.HALF;
            final float quarterWidth = width * Quantity.QUARTER;
            final float quarterHeight = height * Quantity.QUARTER;

            final float overallProgress = cell.animationProgress;
            float part1Progress = Quantity.FULL;
            float part2Progress = Quantity.FULL;
            if (overallProgress < Quantity.HALF) {
                part1Progress = overallProgress / Quantity.HALF;
                part2Progress = Quantity.NONE;
            } else if (overallProgress < Quantity.FULL) {
                part2Progress = (overallProgress - Quantity.HALF) / Quantity.HALF;
            }

            canvas.drawLine(quarterWidth, quarterHeight, quarterWidth + halfWidth * part1Progress,
                quarterHeight + halfHeight * part1Progress, paint);

            canvas.drawLine(width - quarterWidth, quarterHeight, width - quarterWidth - halfWidth *
                part2Progress, quarterHeight + halfHeight * part2Progress, paint);
        }
    };

    protected static final CellDrawable O_DRAWABLE = new CellDrawable() {
        private static final float START_ANGLE = 270f;
        private static final float SWEEP_ANGLE = 360f;

        @Override
        public boolean isAnimated() {
            return true;
        }

        @Override
        public void draw(FieldCell cell, Canvas canvas) {
            super.draw(cell, canvas);

            final Paint paint = cell.foregroundPaint;
            paint.setColor(cell.oForegroundColor);

            final float width = cell.getWidth();
            final float height = cell.getHeight();
            final float quarterWidth = width * Quantity.QUARTER;
            final float quarterHeight = height * Quantity.QUARTER;

            canvas.drawArc(quarterWidth, quarterHeight, width - quarterWidth, height - quarterHeight,
                START_ANGLE, SWEEP_ANGLE * cell.animationProgress, false, paint);
        }
    };
}
