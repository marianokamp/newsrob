package com.newsrob.widget;

import static java.lang.Math.abs;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.widget.CheckBox;
import android.widget.RelativeLayout;

import com.newsrob.threetosix.R;

public class SwipeRelativeLayout extends RelativeLayout implements OnGestureListener {

	private GestureDetector gestureDetector;

	private ISwipeListener swipeListener;

	// private TouchDelegate buttonDelegate;
	private TouchDelegate checkBoxDelegate;

	// private boolean inTouchButtonDelegate;
	private boolean inTouchCheckBoxDelegate;

	private boolean inSuper;

	private boolean inGestureDetector;

	private boolean swipeEnabled = true;

	public SwipeRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SwipeRelativeLayout(Context context) {
		super(context);
		init();
	}

	private void init() {
		gestureDetector = new GestureDetector(this);
		gestureDetector.setIsLongpressEnabled(true);
	}

	public void setSwipeListener(ISwipeListener swipeListener) {
		this.swipeListener = swipeListener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (!swipeEnabled)
			return super.onTouchEvent(event);

		// l("onTouch", event, false);
		switch (event.getAction()) {
		case MotionEvent.ACTION_CANCEL:
			cancelAllTouchTargets(event);
			// inTouchButtonDelegate = false;
			inTouchCheckBoxDelegate = false;
			inSuper = false;
			inGestureDetector = false;
			return false;
		case MotionEvent.ACTION_DOWN:
			cancelAllTouchTargets(event);

			boolean consumed = false;

			if (checkBoxDelegate != null) {
				consumed = checkBoxDelegate.onTouchEvent(event);
				// l("cb touch delegate", event, consumed);
				if (consumed) {
					inTouchCheckBoxDelegate = true;
					return true;
				}
			}

			consumed = gestureDetector.onTouchEvent(event);
			// l("gesture detector", event, consumed);
			if (consumed) {
				inGestureDetector = true;
				return true;
			}

			consumed = super.onTouchEvent(event);
			// l("super", event, consumed);
			if (consumed) {
				inSuper = true;
				return true;
			}

			break;

		case MotionEvent.ACTION_UP:

			if (inTouchCheckBoxDelegate) {
				inTouchCheckBoxDelegate = false;
				consumed = checkBoxDelegate.onTouchEvent(event);
				// l("cb touch delegate", event, consumed);

				return consumed;
			}
			if (inGestureDetector) {
				inGestureDetector = false;
				consumed = gestureDetector.onTouchEvent(event);
				// l("gesture detector", event, consumed);

				return consumed;
			}
			if (inSuper) {
				inSuper = false;
				consumed = super.onTouchEvent(event);
				// l("super", event, consumed);

				return consumed;
			}
			break;

		case MotionEvent.ACTION_MOVE:

			if (inTouchCheckBoxDelegate) {
				consumed = checkBoxDelegate.onTouchEvent(event);
				// l("cb touch delegate", event, consumed);

				return consumed;
			}
			if (inGestureDetector) {
				consumed = gestureDetector.onTouchEvent(event);
				// l("gesture detector", event, consumed);

				return consumed;
			}
			if (inSuper) {
				consumed = super.onTouchEvent(event);
				// l("super", event, consumed);

				return consumed;
			}
			break;
		}
		return false;
	}

	private void cancelAllTouchTargets(MotionEvent event) {
		MotionEvent cancelEvent = MotionEvent.obtain(event);
		cancelEvent.setAction(MotionEvent.ACTION_CANCEL);

		/* buttonDelegate.onTouchEvent(cancelEvent); */
		if (checkBoxDelegate != null)
			checkBoxDelegate.onTouchEvent(cancelEvent);
		gestureDetector.onTouchEvent(cancelEvent);
		super.onTouchEvent(cancelEvent);

	}

	public boolean onDown(MotionEvent e) {
		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (swipeListener == null)
			return false;

		float xDiff = (e1.getX() - e2.getX());
		float yDiff = (e1.getY() - e2.getY());

		boolean result = false;
		if (abs(xDiff) > abs(yDiff)) {
			if (xDiff < 0)
				result = swipeListener.swipeLeftToRight(this);
			else
				result = swipeListener.swipeRightToLeft(this);
		} else {
			if (yDiff < 0)
				result = swipeListener.swipeTopToBottom(this);
			else
				result = swipeListener.swipeBottomToTop(this);
		}

		return result;
	}

	public void onLongPress(MotionEvent e) {
		if (swipeListener != null)
			swipeListener.onLongClick(this, e);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

		return false;
	}

	public void onShowPress(MotionEvent e) {
	}

	public boolean onSingleTapUp(MotionEvent e) {
		if (swipeListener == null)
			return false;

		return swipeListener.onClick(this, e);

	}

	public interface ISwipeListener {
		boolean swipeLeftToRight(View target);

		boolean swipeRightToLeft(View target);

		boolean swipeTopToBottom(View target);

		boolean swipeBottomToTop(View target);

		boolean onLongClick(View target, MotionEvent e);

		boolean onClick(View target, MotionEvent e);

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		Rect outRect;
		int width, height;

		CheckBox checkBox = (CheckBox) findViewById(R.id.star_checkbox);
		if (checkBox != null) {

			outRect = new Rect();
			int[] xy = new int[2];
			checkBox.getLocationInWindow(xy);
			checkBox.getDrawingRect(outRect);
			width = outRect.right - outRect.left;
			height = outRect.bottom - outRect.top;
			outRect.left = xy[0];
			outRect.top = 0;// xy[1];
			outRect.right = outRect.left + width;
			outRect.bottom = outRect.top + height;
			outRect.left -= width;
			outRect.bottom += height;

			checkBoxDelegate = new TouchDelegate(outRect, checkBox);
		}

	}

	public void setSwipeEnabeld(boolean swipeEnabled) {
		this.swipeEnabled = swipeEnabled;
	}

}
