package com.newsrob.widget;

import static java.lang.Math.abs;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class GestureView extends View {

    private ListView listView;
    private boolean inDown = false;
    private float startX;
    private float startY;

    public GestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GestureView(Context context) {
        super(context);
        init();
    }

    private void init() {
    }

    private ListView getListView() {
        if (listView == null)
            listView = (ListView) ((ViewGroup) getParent()).getChildAt(0);
        return listView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (true)
            return super.onTouchEvent(event);

        switch (event.getAction()) {
        case MotionEvent.ACTION_CANCEL:
            inDown = false;
            startX = 0f;
            startY = 0f;
            return super.onTouchEvent(event);

        case MotionEvent.ACTION_MOVE:
            return super.onTouchEvent(event) || inDown;

        case MotionEvent.ACTION_UP:
            String flingDirection = getFling(startX, startY, event.getX(), event.getY());

            inDown = false;

            startX = 0f;
            startY = 0f;

            if (flingDirection != null) {
                MotionEvent cancelEvent = MotionEvent.obtain(event);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                boolean result = super.onTouchEvent(cancelEvent);
                return result;
            } else {
                boolean result = super.onTouchEvent(event);
                return result;
            }
        case MotionEvent.ACTION_DOWN:
            inDown = true;
            startX = event.getX();
            startY = event.getY();
            boolean result = super.onTouchEvent(event);
            return false;

        }
        return super.onTouchEvent(event);
    }

    private String getFling(float x1, float y1, float x2, float y2) {
        float xDiff = (x1 - x2);
        float yDiff = (y1 - y2);

        float travel = Math.abs(xDiff) + Math.abs(yDiff);

        if (travel < 50f)
            return null;

        String direction = null;
        if (abs(xDiff) > abs(yDiff)) {
            if (xDiff < 0)
                direction = "LTR";
            else
                direction = "RTL";
        } else {
            if (yDiff < 0)
                direction = "TTB";
            else
                direction = "BTT";
        }
        return direction;
    }

}
