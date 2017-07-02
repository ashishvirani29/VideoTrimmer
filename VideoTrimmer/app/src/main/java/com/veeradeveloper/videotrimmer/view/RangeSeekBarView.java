/*
 * MIT License
 *
 * Copyright (c) 2016 Knowledge, education for life.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.veeradeveloper.videotrimmer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.veeradeveloper.videotrimmer.R;
import com.veeradeveloper.videotrimmer.Thumb;
import com.veeradeveloper.videotrimmer.interfaces.OnRangeSeekBarListener;

import java.util.ArrayList;
import java.util.List;


public class RangeSeekBarView extends View {

    private static final String TAG;
    private int currentThumb;
    private boolean mFirstRun;
    private int mHeightTimeLine;
    private final Paint mLine;
    private List<OnRangeSeekBarListener> mListeners;
    private float mMaxWidth;
    private float mPixelRangeMax;
    private float mPixelRangeMin;
    private float mScaleRangeMax;
    private final Paint mShadow;
    private float mThumbHeight;
    private float mThumbWidth;
    private List<Thumb> mThumbs;
    private int mViewWidth;

    static {
        TAG = RangeSeekBarView.class.getSimpleName();
    }

    public RangeSeekBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangeSeekBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mShadow = new Paint();
        this.mLine = new Paint();
        this.currentThumb = 0;
        init();
    }

    private void init() {
        this.mThumbs = Thumb.initThumbs(getResources());
        this.mThumbWidth = (float) Thumb.getWidthBitmap(this.mThumbs);
        this.mThumbHeight = 10.0f;
        this.mScaleRangeMax = 100.0f;
        this.mHeightTimeLine = getContext().getResources().getDimensionPixelOffset(R.dimen.frames_video_height);
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.mFirstRun = true;
        int shadowColor = Color.parseColor("#FF000000");
        this.mShadow.setAntiAlias(true);
        this.mShadow.setColor(shadowColor);
        this. mShadow.setAlpha(177);
        int lineColor = Color.parseColor("#FF15FF00");
        this.mLine.setAntiAlias(true);
        this.mLine.setColor(lineColor);
        this.mLine.setAlpha(200);
    }

    public void initMaxWidth() {
        this.mMaxWidth = ((Thumb) this.mThumbs.get(1)).getPos() - ((Thumb) this.mThumbs.get(0)).getPos();
        onSeekStop(this, 0, ((Thumb) this.mThumbs.get(0)).getVal());
        onSeekStop(this, 1, ((Thumb) this.mThumbs.get(1)).getVal());
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mViewWidth = resolveSizeAndState((getPaddingLeft() + getPaddingRight()) + getSuggestedMinimumWidth(), widthMeasureSpec, 1);
        setMeasuredDimension(this.mViewWidth, resolveSizeAndState(((getPaddingBottom() + getPaddingTop()) + ((int) this.mThumbHeight)) + this.mHeightTimeLine, heightMeasureSpec, 1));
        this.mPixelRangeMin = 0.0f;
        this.mPixelRangeMax = ((float) this.mViewWidth) - this.mThumbWidth;
        if (this.mFirstRun) {
            for (int i = 0; i < this.mThumbs.size(); i++) {
                Thumb th = (Thumb) this.mThumbs.get(i);
                th.setVal(this.mScaleRangeMax * ((float) i));
                th.setPos(this.mPixelRangeMax * ((float) i));
            }
            onCreate(this, this.currentThumb, getThumbValue(this.currentThumb));
            this.mFirstRun = false;
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawShadow(canvas);
        drawThumbs(canvas);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        float coordinate = ev.getX();
        Thumb mThumb;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:/*0*/
                this.currentThumb = getClosestThumb(coordinate);
                if (this.currentThumb == -1) {
                    return false;
                }
                mThumb = (Thumb) this.mThumbs.get(this.currentThumb);
                mThumb.setLastTouchX(coordinate);
                onSeekStart(this, this.currentThumb, mThumb.getVal());
                return true;
            case MotionEvent.ACTION_UP:
                if (this.currentThumb == -1) {
                    return false;
                }
                onSeekStop(this, this.currentThumb, ((Thumb) this.mThumbs.get(this.currentThumb)).getVal());
                return true;
            case MotionEvent.ACTION_MOVE:
                int i;
                mThumb = (Thumb) this.mThumbs.get(this.currentThumb);
                List list = this.mThumbs;
                if (this.currentThumb == 0) {
                    i = 1;
                } else {
                    i = 0;
                }
                Thumb mThumb2 = (Thumb) list.get(i);
                float dx = coordinate - mThumb.getLastTouchX();
                float newX = mThumb.getPos() + dx;
                if (this.currentThumb == 0) {
                    if (((float) mThumb.getWidthBitmap()) + newX >= mThumb2.getPos()) {
                        mThumb.setPos(mThumb2.getPos() - ((float) mThumb.getWidthBitmap()));
                    } else if (newX <= this.mPixelRangeMin) {
                        mThumb.setPos(this.mPixelRangeMin);
                    } else {
                        checkPositionThumb(mThumb, mThumb2, dx, true);
                        mThumb.setPos(mThumb.getPos() + dx);
                        mThumb.setLastTouchX(coordinate);
                    }
                } else if (newX <= mThumb2.getPos() + ((float) mThumb2.getWidthBitmap())) {
                    mThumb.setPos(mThumb2.getPos() + ((float) mThumb.getWidthBitmap()));
                } else if (newX >= this.mPixelRangeMax) {
                    mThumb.setPos(this.mPixelRangeMax);
                } else {
                    checkPositionThumb(mThumb2, mThumb, dx, false);
                    mThumb.setPos(mThumb.getPos() + dx);
                    mThumb.setLastTouchX(coordinate);
                }
                setThumbPos(this.currentThumb, mThumb.getPos());
                invalidate();
                return true;
            default:
                return false;
        }
    }

    private void checkPositionThumb(Thumb mThumbLeft, Thumb mThumbRight, float dx, boolean isLeftMove) {
        if (!isLeftMove || dx >= 0.0f) {
            if (!isLeftMove && dx > 0.0f && (mThumbRight.getPos() + dx) - mThumbLeft.getPos() > this.mMaxWidth) {
                mThumbLeft.setPos((mThumbRight.getPos() + dx) - this.mMaxWidth);
                setThumbPos(0, mThumbLeft.getPos());
            }
        } else if (mThumbRight.getPos() - (mThumbLeft.getPos() + dx) > this.mMaxWidth) {
            mThumbRight.setPos((mThumbLeft.getPos() + dx) + this.mMaxWidth);
            setThumbPos(1, mThumbRight.getPos());
        }
    }

    private int getUnstuckFrom(int index) {
        float lastVal = ((Thumb) this.mThumbs.get(index)).getVal();
        for (int i = index - 1; i >= 0; i--) {
            if (((Thumb) this.mThumbs.get(i)).getVal() != lastVal) {
                return i + 1;
            }
        }
        return 0;
    }

    private float pixelToScale(int index, float pixelValue) {
        float scale = (pixelValue * 100.0f) / this.mPixelRangeMax;
        if (index == 0) {
            return ((((this.mThumbWidth * scale) / 100.0f) * 100.0f) / this.mPixelRangeMax) + scale;
        }
        return scale - (((((100.0f - scale) * this.mThumbWidth) / 100.0f) * 100.0f) / this.mPixelRangeMax);
    }

    private float scaleToPixel(int index, float scaleValue) {
        float px = (this.mPixelRangeMax * scaleValue) / 100.0f;
        if (index == 0) {
            return px - ((this.mThumbWidth * scaleValue) / 100.0f);
        }
        return px + (((100.0f - scaleValue) * this.mThumbWidth) / 100.0f);
    }

    private void calculateThumbValue(int index) {
        if (index < this.mThumbs.size() && !this.mThumbs.isEmpty()) {
            Thumb th = (Thumb) this.mThumbs.get(index);
            th.setVal(pixelToScale(index, th.getPos()));
            onSeek(this, index, th.getVal());
        }
    }

    private void calculateThumbPos(int index) {
        if (index < this.mThumbs.size() && !this.mThumbs.isEmpty()) {
            Thumb th = (Thumb) this.mThumbs.get(index);
            th.setPos(scaleToPixel(index, th.getVal()));
        }
    }

    private float getThumbValue(int index) {
        return ((Thumb) this.mThumbs.get(index)).getVal();
    }

    public void setThumbValue(int index, float value) {
        ((Thumb) this.mThumbs.get(index)).setVal(value);
        calculateThumbPos(index);
        invalidate();
    }

    private void setThumbPos(int index, float pos) {
        ((Thumb) this.mThumbs.get(index)).setPos(pos);
        calculateThumbValue(index);
        invalidate();
    }

    private int getClosestThumb(float coordinate) {
        int closest = -1;
        if (!this.mThumbs.isEmpty()) {
            for (int i = 0; i < this.mThumbs.size(); i++) {
                float tcoordinate = ((Thumb) this.mThumbs.get(i)).getPos() + this.mThumbWidth;
                if (coordinate >= ((Thumb) this.mThumbs.get(i)).getPos() && coordinate <= tcoordinate) {
                    closest = ((Thumb) this.mThumbs.get(i)).getIndex();
                }
            }
        }
        return closest;
    }

    private void drawShadow(Canvas canvas) {
        if (!this.mThumbs.isEmpty()) {
            for (Thumb th : this.mThumbs) {
                float x;
                if (th.getIndex() == 0) {
                    x = th.getPos() + ((float) getPaddingLeft());
                    if (x > this.mPixelRangeMin) {
                        canvas.drawRect(new Rect((int) this.mThumbWidth, 0, (int) (this.mThumbWidth + x), this.mHeightTimeLine), this.mShadow);
                    }
                } else {
                    x = th.getPos() - ((float) getPaddingRight());
                    if (x < this.mPixelRangeMax) {
                        canvas.drawRect(new Rect((int) x, 0, (int) (((float) this.mViewWidth) - this.mThumbWidth), this.mHeightTimeLine), this.mShadow);
                    }
                }
            }
        }
    }

    private void drawThumbs(Canvas canvas) {
        if (!this.mThumbs.isEmpty()) {
            for (Thumb th : this.mThumbs) {
                if (th.getIndex() == 0) {
                    canvas.drawBitmap(th.getBitmap(), th.getPos() + ((float) getPaddingLeft()), (float) getPaddingTop(), null);
                } else {
                    canvas.drawBitmap(th.getBitmap(), th.getPos() - ((float) getPaddingRight()), (float) getPaddingTop(), null);
                }
            }
        }
    }

    public void addOnRangeSeekBarListener(OnRangeSeekBarListener listener) {
        if (this.mListeners == null) {
            this.mListeners = new ArrayList();
        }
        this.mListeners.add(listener);
    }

    private void onCreate(RangeSeekBarView rangeSeekBarView, int index, float value) {
        if (this.mListeners != null) {
            for (OnRangeSeekBarListener item : this.mListeners) {
                item.onCreate(rangeSeekBarView, index, value);
            }
        }
    }

    private void onSeek(RangeSeekBarView rangeSeekBarView, int index, float value) {
        if (this.mListeners != null) {
            for (OnRangeSeekBarListener item : this.mListeners) {
                item.onSeek(rangeSeekBarView, index, value);
            }
        }
    }

    private void onSeekStart(RangeSeekBarView rangeSeekBarView, int index, float value) {
        if (this.mListeners != null) {
            for (OnRangeSeekBarListener item : this.mListeners) {
                item.onSeekStart(rangeSeekBarView, index, value);
            }
        }
    }

    private void onSeekStop(RangeSeekBarView rangeSeekBarView, int index, float value) {
        if (this.mListeners != null) {
            for (OnRangeSeekBarListener item : this.mListeners) {
                item.onSeekStop(rangeSeekBarView, index, value);
            }
        }
    }

    public List<Thumb> getThumbs() {
        return this.mThumbs;
    }
}
