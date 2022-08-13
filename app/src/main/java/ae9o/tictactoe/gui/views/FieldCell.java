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
import ae9o.tictactoe.game.Mark;
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

/**
 * {@link View} which implements a field cell with the ability to display animated marks.
 *
 * <p>Works in tandem with the {@link FieldLayout}. But in case of emergency, it can be used standalone.
 */
public class FieldCell extends View {
    /** The standard line width for marks inside a cell. */
    private static final int DEFAULT_STROKE_WIDTH = 20;

    /** Parent layout. */
    private FieldLayout fieldLayout;
    /** Current mark of the cell. */
    private Mark mark = Mark.EMPTY;
    /** Cell's coordinates. */
    private int row;
    private int col;

    /** Lots of stuff to draw animated marks. */
    private Paint backgroundPaint;
    private Paint foregroundPaint;
    private int activeBackgroundColor = Color.LTGRAY;
    private int inactiveBackgroundColor = Color.WHITE;
    private float animationProgress;
    private CellDrawable drawable = EMPTY_DRAWABLE;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     */
    public FieldCell(Context context) {
        this(context, null, 0, 0);
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
    public FieldCell(Context context, @Nullable AttributeSet attrs) {
        this(context,  attrs, 0, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a
     * theme attribute. This constructor of View allows subclasses to use their
     * own base style when they are inflating. For example, a Button class's
     * constructor would call this version of the super class constructor and
     * supply <code>R.attr.buttonStyle</code> for <var>defStyleAttr</var>; this
     * allows the theme's button style to modify all of the base view attributes
     * (in particular its background) as well as the Button class's attributes.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     */
    public FieldCell(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a
     * theme attribute or style resource. This constructor of View allows
     * subclasses to use their own base style when they are inflating.
     *
     * <p>When determining the final value of a particular attribute, there are
     * four inputs that come into play:
     * <ol>
     * <li>Any attribute values in the given AttributeSet.
     * <li>The style resource specified in the AttributeSet (named "style").
     * <li>The default style specified by <var>defStyleAttr</var>.
     * <li>The default style specified by <var>defStyleRes</var>.
     * <li>The base values in this theme.
     * </ol>
     *
     * <p>Each of these inputs is considered in-order, with the first listed taking
     * precedence over the following ones. In other words, if in the
     * AttributeSet you have supplied <code>&lt;Button * textColor="#ff000000"&gt;</code>
     * , then the button's text will <em>always</em> be black, regardless of
     * what is specified in any of the styles.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @param defStyleRes A resource identifier of a style resource that
     *        supplies default values for the view, used only if
     *        defStyleAttr is 0 or can not be found in the theme. Can be 0
     *        to not look for defaults.
     */
    public FieldCell(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(inactiveBackgroundColor);

        foregroundPaint = new Paint();
        foregroundPaint.setStyle(Paint.Style.STROKE);
        foregroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        foregroundPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);

        parseAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Initializes the view with parameters retrieved from the given XML attributes.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @param defStyleRes A resource identifier of a style resource that
     *        supplies default values for the view, used only if
     *        defStyleAttr is 0 or can not be found in the theme. Can be 0
     *        to not look for defaults.
     */
    private void parseAttributes(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) {
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FieldCell, defStyleAttr, defStyleRes);
        try {
            setForegroundColor(a.getColor(R.styleable.FieldCell_foregroundColor, 0));
            setMark(Mark.valueOf(a.getInt(R.styleable.FieldCell_mark, 0)));
        } finally {
            a.recycle();
        }
    }

    /**
     * Returns this view's parent layout.
     */
    @Nullable
    public FieldLayout getFieldLayout() {
        return fieldLayout;
    }

    /**
     * Sets this view's parent layout.
     *
     * @param fieldLayout The new parent layout.
     */
    public void setFieldLayout(@Nullable FieldLayout fieldLayout) {
        this.fieldLayout = fieldLayout;
    }

    /**
     * Returns this view's row coordinate.
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns this view's col coordinate.
     */
    public int getCol() {
        return col;
    }

    /**
     * Sets this view's location on the field.
     *
     * @param row The row coordinate.
     * @param col The col coordinate.
     */
    public void setLocation(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Returns this view's foreground color.
     */
    public int getForegroundColor() {
        return foregroundPaint.getColor();
    }

    /**
     * Sets this view's foreground color.
     *
     * @param foregroundColor The new color.
     */
    public void setForegroundColor(int foregroundColor) {
        if (foregroundColor == foregroundPaint.getColor()) {
            return;
        }
        foregroundPaint.setColor(foregroundColor);

        invalidate();
    }

    /**
     * Returns the background color of the cell when the player's finger is placed on it.
     */
    public int getActiveBackgroundColor() {
        return activeBackgroundColor;
    }

    /**
     * Sets the background color of the cell when the player's finger is placed on it.
     *
     * @param activeBackgroundColor The new color.
     */
    public void setActiveBackgroundColor(int activeBackgroundColor) {
        this.activeBackgroundColor = activeBackgroundColor;
    }

    /**
     * Returns the background color of the cell when it is not pushed.
     */
    public int getInactiveBackgroundColor() {
        return inactiveBackgroundColor;
    }

    /**
     * Sets the background color of the cell when it is not pushed.
     *
     * @param inactiveBackgroundColor The new color.
     */
    public void setInactiveBackgroundColor(int inactiveBackgroundColor) {
        this.inactiveBackgroundColor = inactiveBackgroundColor;
    }

    /**
     * Returns the current mark of the cell.
     */
    public Mark getMark() {
        return mark;
    }

    /**
     * Sets current mark of the cell.
     *
     * @param mark The new mark.
     */
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

    /**
     * Reconfigures drawable when the view's mark changes.
     */
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

    /**
     * Animates the appearance of the mark.
     */
    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        }
    };

    /**
     * Starts a new mark appearance animation from the beginning.
     */
    public void startAnimation() {
        animationProgress = Quantity.NONE;

        ValueAnimator animator = ValueAnimator.ofFloat(Quantity.NONE, Quantity.FULL);
        animator.addUpdateListener(animatorUpdateListener);
        animator.start();
    }

    /**
     * Measures the dimensions of the cell when the parent layout changes.
     *
     * <p>The cell is formed square and depends on the width of the view.
     *
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent.
     *                         The requirements are encoded with
     *                         {@link android.view.View.MeasureSpec}.
     * @param heightMeasureSpec vertical space requirements as imposed by the parent.
     *                         The requirements are encoded with
     *                         {@link android.view.View.MeasureSpec}.
     */
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

    /**
     * Draws a cell according to its mark.
     *
     * @param canvas the canvas on which the background will be drawn
     */
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

                default:
                    // Do nothing.
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * Notifies the parent layout when the cell is clicked.
     *
     * @return True there was an assigned OnClickListener that was called, false
     *         otherwise is returned.
     */
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

    /**
     * A base helper class to support the ability to draw various marks.
     */
    protected static class CellDrawable {
        public boolean isAnimated() {
            return false;
        }

        public void draw(FieldCell cell, Canvas canvas) {
            canvas.drawRect(0, 0, cell.getWidth(), cell.getHeight(), cell.backgroundPaint);
        }
    }

    /**
     * Draws an empty cell.
     */
    protected static final CellDrawable EMPTY_DRAWABLE = new CellDrawable();

    /**
     * Draws a cell marked X.
     */
    protected static final CellDrawable X_DRAWABLE = new CellDrawable() {
        @Override
        public boolean isAnimated() {
            return true;
        }

        @Override
        public void draw(FieldCell cell, Canvas canvas) {
            super.draw(cell, canvas);

            final Paint paint = cell.foregroundPaint;

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

    /**
     * Draws a cell marked O.
     */
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

            final float width = cell.getWidth();
            final float height = cell.getHeight();
            final float quarterWidth = width * Quantity.QUARTER;
            final float quarterHeight = height * Quantity.QUARTER;

            canvas.drawArc(quarterWidth, quarterHeight, width - quarterWidth, height - quarterHeight,
                START_ANGLE, SWEEP_ANGLE * cell.animationProgress, false, paint);
        }
    };
}
