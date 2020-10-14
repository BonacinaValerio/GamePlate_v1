package com.bonacogo.gameplate.bottomnav;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.bonacogo.gameplate.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.core.content.ContextCompat;

public class CustomBottomNavigationView extends BottomNavigationView {
    private Path mPath, mPathStroke;
    private Paint mPaint, mPaintLine, mPaintStroke;

    // CURVE_CIRCLE_RADIUS rappresenta il raggio della conca nel bottomnavigationview
    private final int CURVE_CIRCLE_RADIUS = 250 / 2;
    private int spacePx;
    public int getCURVE_CIRCLE_RADIUS() {
        return CURVE_CIRCLE_RADIUS;
    }

    private Point CurveStartPoint0 = new Point();
    final RectF oval0 = new RectF();
    private Point CurveStartPoint = new Point();
    final RectF oval = new RectF();

    private int mNavigationBarWidth, mNavigationBarHeight;

    public CustomBottomNavigationView(Context context) {
        super(context);
        init();
    }

    public CustomBottomNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomBottomNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPath = new Path();
        mPathStroke = new Path();
        mPaint = new Paint();
        mPaintLine = new Paint();
        mPaintStroke = new Paint();

        final float scale = getResources().getDisplayMetrics().density;
        spacePx = (int) (1.4 * scale + 0.5f);

        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintStroke.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintLine.setStyle(Paint.Style.STROKE);
        mPaintLine.setStrokeWidth(spacePx*2);

        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // get width and height
        mNavigationBarWidth = getWidth();
        mNavigationBarHeight = getHeight();
        // coloro la forma
        int color = ContextCompat.getColor(getContext(), R.color.bottomNav);
        int color2 = ContextCompat.getColor(getContext(), R.color.lightGray);
        mPaint.setColor(color);

        mPaintStroke.setColor(color2);
        mPaintLine.setColor(color2);

        // imposto il punto e la forma

        CurveStartPoint0.set((mNavigationBarWidth / 2) - CURVE_CIRCLE_RADIUS, 0);
        oval0.set((mNavigationBarWidth / 2) - CURVE_CIRCLE_RADIUS,
                0-CURVE_CIRCLE_RADIUS ,
                (mNavigationBarWidth / 2) + CURVE_CIRCLE_RADIUS,
                CURVE_CIRCLE_RADIUS);
        mPathStroke.reset();
        mPathStroke.moveTo(0, 0);
        mPathStroke.lineTo(CurveStartPoint0.x, CurveStartPoint0.y);
        // disegno l'arco
        mPathStroke.arcTo(oval0, 180, -180);
        mPathStroke.lineTo(mNavigationBarWidth, 0);
        mPathStroke.lineTo(mNavigationBarWidth, mNavigationBarHeight);
        mPathStroke.lineTo(0, mNavigationBarHeight);
        mPathStroke.close();


        CurveStartPoint.set((mNavigationBarWidth / 2) - CURVE_CIRCLE_RADIUS - spacePx, 0);
        oval.set((mNavigationBarWidth / 2) - CURVE_CIRCLE_RADIUS - spacePx,
                0-CURVE_CIRCLE_RADIUS - spacePx,
                (mNavigationBarWidth / 2) + CURVE_CIRCLE_RADIUS + spacePx,
                CURVE_CIRCLE_RADIUS + spacePx);

        // traccio la forma
        mPath.reset();
        mPath.moveTo(0, 0);
        mPath.lineTo(CurveStartPoint.x, CurveStartPoint.y);
        // disegno l'arco
        mPath.arcTo(oval, 180, -180);
        mPath.lineTo(mNavigationBarWidth, 0);
        mPath.lineTo(mNavigationBarWidth, mNavigationBarHeight);
        mPath.lineTo(0, mNavigationBarHeight);
        mPath.close();




    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(mPathStroke, mPaintStroke);
        canvas.drawPath(mPath, mPaint);
        canvas.drawLine(0,0, CurveStartPoint.x, CurveStartPoint.y, mPaintLine);
        canvas.drawLine((mNavigationBarWidth / 2) + CURVE_CIRCLE_RADIUS,0, mNavigationBarWidth, 0, mPaintLine);
    }

}
