package com.newsrob.widget;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.newsrob.EntryManager;

public class RelativeLayout extends android.widget.RelativeLayout {

    private Button next;
    private Button prev;

    private Runnable hideControlsRunnable;

    private boolean isShowingControls;

    private Handler handler = new Handler();

    private float minHeightTouchableArea;
    private float maxHeightTouchableArea;

    private EntryManager entryManager;

    public RelativeLayout(Context context) {
        super(context);
        init(context);
    }

    public RelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        hideControlsRunnable = new Runnable() {
            public void run() {
                hideControls();
            }
        };
        entryManager = EntryManager.getInstance(context);
    }

    public void setNextButton(Button next) {
        this.next = next;
    }

    public void setPrevButton(Button prev) {
        this.prev = prev;
    }

    @Override
    public final boolean dispatchTouchEvent(MotionEvent ev) {
        if (!isShowingControls && ev.getAction() == MotionEvent.ACTION_DOWN) {
            float y = ev.getY();
            if (y > minHeightTouchableArea && y < maxHeightTouchableArea)
                showControls();
        }
        try {
            return super.dispatchTouchEvent(ev);
        } catch (NullPointerException npe) {
            return false;
        }
    }

    private void showControls() {

        if (!entryManager.isHoveringButtonsNavigationEnabled())
            return;

        if (next == null || prev == null)
            return;

        if (next.isEnabled())
            next.setVisibility(View.VISIBLE);

        if (prev.isEnabled())
            prev.setVisibility(View.VISIBLE);

        handler.removeCallbacks(hideControlsRunnable);
        handler.postDelayed(hideControlsRunnable, 1500);

        isShowingControls = true;
    }

    private void hideControls() {
        if (next == null || prev == null)
            return;

        next.setVisibility(View.GONE);
        prev.setVisibility(View.GONE);

        isShowingControls = false;
    }

    public void updateState(boolean nextAvailable, boolean previousAvailable) {
        next.setEnabled(nextAvailable);
        prev.setEnabled(previousAvailable);

        evaluateButtonsVisibility();
    }

    private void evaluateButtonsVisibility() {
        if (isShowingControls) {
            next.setVisibility(next.isEnabled() ? View.VISIBLE : View.INVISIBLE);
            prev.setVisibility(prev.isEnabled() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int height = getHeight();
        minHeightTouchableArea = height * 0.2f;
        maxHeightTouchableArea = height * 0.8f;
    }

}
