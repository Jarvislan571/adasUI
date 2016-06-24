package com.tencent.adasdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * @author xiaojunzhou
 * @date 16/6/8
 */
public class CoverView extends ImageView{
    private Paint paintCar;
    private Paint paintLane;
    private Paint paintText;
    private Paint paintRegion;

    private ArrayList<Path> paths = null;
    private ArrayList<RectF> mRects  = null;
    private ArrayList<RectF> mRegion  = null;
    private ArrayList<String>   mTexts  = null;

    public CoverView(Context context) {
        super(context);
        init();
    }

    public CoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void addRect(RectF rects, String text) {
        mRects.add(rects);
        mTexts.add(text);
    }

    public void addRect(RectF rects) {
        mRegion.add(rects);
    }

    public void addPath(ArrayList<Path> paths) {
        this.paths = paths;
    }

    private void init() {
        paintCar = new Paint();
        paintCar.setColor(Color.RED);
        paintCar.setStyle(Paint.Style.STROKE);
        paintCar.setStrokeWidth(5);

        paintLane = new Paint();
        paintLane.setColor(Color.GREEN);
        paintLane.setStyle(Paint.Style.STROKE);
        paintLane.setStrokeWidth(2);

        paintText   = new Paint();
        paintText.setColor(Color.GREEN);
        paintText.setStyle(Paint.Style.FILL);
        paintText.setTextSize(30);


        paintRegion   = new Paint();
        paintRegion.setColor(Color.BLUE);
        paintRegion.setStyle(Paint.Style.STROKE);
        paintRegion.setStrokeWidth(2);
        paintRegion.setTextSize(30);

        mRects  = new ArrayList<RectF>();
        mTexts  = new ArrayList<String>();
        mRegion = new ArrayList<RectF>();
    }
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (paths != null && paths.size() > 0) {
            for (Path path : paths) {
                canvas.drawPath(path, paintLane);
            }
        }

        if (!mRects.isEmpty()) {
            for ( int i=0; i<mRects.size(); ++i) {
                RectF rect = mRects.get(i);
                canvas.drawRect(rect, paintCar);

                String text = mTexts.get(i);
                if (null != text) {
                    canvas.drawText(text,
                            rect.left + rect.width() / 4,
                            rect.top + rect.height() / 2,
                            paintText
                    );
                }
            }
        }

        if (!mRegion.isEmpty()) {
            for ( int i=0; i<mRegion.size(); ++i) {
                RectF rect = mRegion.get(i);
                canvas.drawRect(rect, paintRegion);
            }
        }

    }

    public void clear() {
        mRects.clear();
        mTexts.clear();
        mRegion.clear();
    }
//    public void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        if (rect != null) {
//            if (!mIsLane)
//                canvas.drawRect(rect, paint);
//            else {
//                canvas.drawLine(rect.left, rect.top, rect.right, rect.bottom, paint);
//            }
//        }
//        if (!TextUtils.isEmpty(tpaintCarext)) {
//            paint.setColor(Color.GREEN);
//            paint.setStyle(Paint.Style.FILL);
//            paint.setTextSize(30);
//            canvas.drawText(text,
//                    rect.width() / 4,
//                    rect.height() / 2,
//                    paint
//            );
//        }
//    }
}
