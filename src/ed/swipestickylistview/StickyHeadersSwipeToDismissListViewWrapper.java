package ed.swipestickylistview;

import java.lang.reflect.Field;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;

public class StickyHeadersSwipeToDismissListViewWrapper extends FrameLayout {

	private static final boolean HONEYCOMB_OR__ABOVE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	private static Field mTop;
	private static Field mBottom;

	private View header = null;
	private int headerBottomPosition = -1;
	private Drawable selector;
	private ViewConfiguration viewConfig;
	private boolean showSelector;
	private boolean drawSelectorOnTop = false;
	private Rect selectorBounds = new Rect();

	private GestureDetector gestureDetector = new GestureDetector(getContext(),
			new GestureDetector.SimpleOnGestureListener() {

				@Override
				public void onShowPress(MotionEvent e) {
					showSelector = true;
					invalidate(getRefreshedSelectorBounds());
				}

			});

	private OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
		@Override
		public void onGlobalLayout() {
			if (headerChangedDuringLayout) {
				if (getChildCount() > 1) {
					removeViewAt(1);
				}
				if (header != null) {
					addView(header);
				}
			}
			headerChangedDuringLayout = false;
		}
	};

	private boolean inLayout;
	private boolean headerChangedDuringLayout;

	public StickyHeadersSwipeToDismissListViewWrapper(Context context) {
		this(context, null, 0);
	}

	public StickyHeadersSwipeToDismissListViewWrapper(Context context,
			AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public StickyHeadersSwipeToDismissListViewWrapper(Context context,
			AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (!HONEYCOMB_OR__ABOVE) {
			try {
				mTop = View.class.getDeclaredField("mTop");
				mBottom = View.class.getDeclaredField("mBottom");
				mTop.setAccessible(true);
				mBottom.setAccessible(true);
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
		viewConfig = ViewConfiguration.get(context);

		getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
	}

	void setHeader(View header) {
		if (header == this.header) {
			return;
		}
		if (this.header != null) {
			throw new IllegalStateException(
					"You must first remove the old header first");
		}
		this.header = header;
		if (header != null) {
			View list = getChildAt(0);
			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
			params.leftMargin = list.getPaddingLeft();
			params.rightMargin = list.getPaddingRight();
			params.gravity = Gravity.TOP;
			header.setLayoutParams(params);

			if (inLayout) {
				headerChangedDuringLayout = true;
			} else {
				addView(header);
			}
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		inLayout = true;
		super.onLayout(changed, left, top, right, bottom);
		setHeaderBottomPosition(this.headerBottomPosition);
		inLayout = false;
	}

	View removeHeader() {
		if (this.header != null) {
			if (inLayout) {
				headerChangedDuringLayout = true;
			} else {
				removeView(this.header);
			}

		}
		View header = this.header;
		this.header = null;
		return header;
	}

	boolean hasHeader() {
		return header != null;
	}

	boolean isHeader(View v) {
		return header == v;
	}

	int getHeaderHeight() {
		if (header == null) {
			return 0;
		}
		MarginLayoutParams params = (MarginLayoutParams) header
				.getLayoutParams();
		int width = getMeasuredWidth()
				- (params == null ? 0
						: (params.leftMargin + params.rightMargin));
		int parentWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width,
				MeasureSpec.EXACTLY);
		int parentHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getHeight(),
				MeasureSpec.EXACTLY);
		measureChild(header, parentWidthMeasureSpec, parentHeightMeasureSpec);
		return header.getMeasuredHeight();
	}

	@SuppressLint("NewApi")
	void setHeaderBottomPosition(int headerBottomPosition) {
		if (header != null) {
			if (HONEYCOMB_OR__ABOVE) {
				header.setTranslationY(headerBottomPosition
						- header.getMeasuredHeight());
			} else {
				try {
					mTop.set(header,
							headerBottomPosition - header.getMeasuredHeight());
					mBottom.set(header, headerBottomPosition);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		this.headerBottomPosition = headerBottomPosition;
	}

	int getHeaderBottomPosition() {
		return headerBottomPosition;
	}

	public void setSelector(Drawable selector) {
		this.selector = selector;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		if (selector != null && showSelector && !drawSelectorOnTop) {
			drawSelector(canvas);
		}
		super.dispatchDraw(canvas);
		if (selector != null && showSelector && drawSelectorOnTop) {
			drawSelector(canvas);
		}
	}

	private void drawSelector(Canvas canvas) {
		selector.setBounds(getRefreshedSelectorBounds());
		int[] selectorState = selector.getState();
		selector.setState(header.getDrawableState());
		selector.draw(canvas);
		selector.setState(selectorState);
	}

	private Rect getRefreshedSelectorBounds() {
		selectorBounds.left = header.getLeft();
		selectorBounds.top = headerBottomPosition - header.getHeight();
		selectorBounds.right = header.getRight();
		selectorBounds.bottom = headerBottomPosition;
		return selectorBounds;
	}

	public void setDrawSelectorOnTop(boolean onTop) {
		this.drawSelectorOnTop = onTop;
	}

}
