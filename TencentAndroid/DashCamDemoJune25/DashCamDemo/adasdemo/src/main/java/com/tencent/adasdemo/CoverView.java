package com.tencent.adasdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.tencent.adas.CarDistance;
import com.tencent.adas.Lane;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * @author xiaojunzhou
 * @date 16/6/8
 */
public class CoverView extends ImageView {
    private static final int marginRight = 10;
    private static final int marginTop = 8;
    private static final int rectWidth = 200;
    private static final int rectHeight = 200;
    private static final int marginLeft = 10;

    private float widthRatio;
    private float heightRatio;
    private ArrayList<CarDistance> carDistances = new ArrayList<>();
    private ArrayList<Lane> lanes = new ArrayList<>();
    private Paint paintRect;
    private Paint paintLane;
    private Paint paintDesc;
    private Paint paintTip;
    private Paint paintCircle;

    public void setRatio(float widthRatio, float heightRatio) {
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
    }


    public void addCarDistance(CarDistance carDistance) {
        carDistances.add(carDistance);
    }

    public void addLane(Lane lane) {
        lanes.add(lane);
    }

    public CoverView(Context context) {
        super(context);
        init();
    }

    public CoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintRect = new Paint();//定义一个画笔

        paintLane = new Paint();
        paintLane.setColor(Color.GREEN);
        paintLane.setStyle(Paint.Style.STROKE);//实线
        paintLane.setStrokeWidth(2);//线宽

        paintDesc = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDesc.setColor(Color.WHITE);
        paintDesc.setStyle(Paint.Style.FILL);
        paintDesc.setTextAlign(Paint.Align.CENTER);
        paintDesc.setTextSize(50);

        paintTip = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTip.setTextSize(60);
        paintTip.setColor(Color.YELLOW);
        paintTip.setStyle(Paint.Style.FILL);
        paintTip.setTextAlign(Paint.Align.CENTER);

        paintCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCircle.setColor(Color.WHITE);
        paintCircle.setTextAlign(Paint.Align.CENTER);
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setStrokeWidth(10);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 车道 ROI
        drawLaneArea(canvas, 480, 756, 1478, 1080);
        // 车辆ROI
        drawLaneArea(canvas, 1920 * 0.3f, 1080 * 0.4f, 1920 * 0.7f, 1080 * 0.8f);

        drawCarDistance(canvas);

        drawLanes(canvas);
    }

    //  ROI区域
    public void drawLaneArea(Canvas canvas, float left, float top, float right, float bottom) {

        RectF rect = new RectF();

        float widthRatio = (float) this.getWidth() / 1920;
        float heightRatio = (float) this.getHeight() / 1080;
        rect.left = left * widthRatio;
        rect.right = right * widthRatio;
        rect.top = top * heightRatio;
        rect.bottom = bottom * heightRatio;

        Paint ptLaneArea = new Paint();
        ptLaneArea.setColor(Color.BLUE);
        ptLaneArea.setAlpha(100);
        ptLaneArea.setStrokeWidth(5);
        ptLaneArea.setStyle(Paint.Style.STROKE);
        canvas.drawRect(rect, ptLaneArea);
    }

    /**
     * 画车辆检测结果
     *
     * @param canvas
     */
    public void drawCarDistance(Canvas canvas) {
        for (int i = 0; i < carDistances.size(); i++) {
            CarDistance item = carDistances.get(i);
            String tip = null;
            if (item.distance > 20) {
                paintRect.setColor(Color.WHITE);
            } else if (item.distance > 10) {
                paintRect.setColor(Color.YELLOW);
                paintTip.setColor(Color.YELLOW);
                tip = "建议安全距离150米";
            } else {
                paintRect.setColor(Color.RED);
                paintTip.setColor(Color.RED);
                tip = "刹车";
            }

            RectF carRect = new RectF(item.x / widthRatio,
                    item.y / heightRatio,
                    (item.x + item.width) / widthRatio,
                    (item.y + item.height) / heightRatio
            );

            int top = (marginTop + (marginTop + rectHeight) * i);
            int left = (this.getRight() - marginRight - rectWidth);
            int right = (this.getRight() - marginRight);
            int bottom = (marginTop + rectHeight + (marginTop + rectHeight) * i);

            RectF descRect = new RectF(left, top, right, bottom);

            Path line = new Path();
            line.moveTo(carRect.right, carRect.top);
            line.lineTo(descRect.left, descRect.bottom);

            // 先画background
            paintRect.setAlpha(80);
            paintRect.setStyle(Paint.Style.FILL);
            canvas.drawRect(carRect, paintRect);
            canvas.drawRect(descRect, paintRect);

            // 再画border
            paintRect.setAlpha(255);
            paintRect.setStyle(Paint.Style.STROKE);
            paintRect.setStrokeWidth(3);
            canvas.drawRect(carRect, paintRect);
            canvas.drawRect(descRect, paintRect);
            canvas.drawPath(line, paintRect);


            // 第二行 车距文字居中，不动
            paintDesc.setTextSize(50);
            Paint.FontMetricsInt fontMetrics = paintDesc.getFontMetricsInt();
            int baseline = (int) ((descRect.top + descRect.top + descRect.height())
                    - (fontMetrics.bottom + fontMetrics.top)) / 2;
            canvas.drawText(item.distance + "米", descRect.centerX(), baseline, paintDesc);

            // 第一行 文字”车辆“
            paintDesc.setTextSize(40);
            fontMetrics = paintDesc.getFontMetricsInt();
            baseline = (int) ((descRect.top + descRect.top + descRect.height() / 3)
                    - (fontMetrics.bottom + fontMetrics.top)) / 2;
            canvas.drawText("车辆", descRect.centerX(), baseline, paintDesc);

            // 第三行 TTC
            if (item.timeToCrash > 0 && item.timeToCrash < 20) {
                DecimalFormat format = new DecimalFormat("##0.00");
                canvas.drawText(format.format(item.timeToCrash) + "s", descRect.centerX(), descRect.bottom - 20, paintDesc);
            }

            fontMetrics = paintTip.getFontMetricsInt();
            if (!TextUtils.isEmpty(tip)) {
                canvas.drawText(tip, this.getRight() / 2, 50 + (50 + fontMetrics.bottom - fontMetrics.top) * i, paintTip);
            }
        }
    }

    /**
     * 画车道线
     *
     * @param canvas
     */
    public void drawLanes(Canvas canvas) {
        if (lanes.size() != 2) {
            return;
        }

        Lane lane1 = lanes.get(0);
        Lane lane2 = lanes.get(1);
        Path path = new Path();
        path.moveTo(lane1.x1 / widthRatio, lane1.y1 / heightRatio);
        path.lineTo(lane1.x2 / widthRatio, lane1.y2 / heightRatio);
        path.lineTo(lane2.x2 / widthRatio, lane2.y2 / heightRatio);
        path.lineTo(lane2.x1 / widthRatio, lane2.y1 / heightRatio);
        path.lineTo(lane1.x1 / widthRatio, lane1.y1 / heightRatio);
        canvas.drawPath(path, paintLane);

        if (lane1.departure == 1) {
            Path p = new Path();
            p.moveTo((lane1.x1) / widthRatio, lane1.y1 / heightRatio);
            p.lineTo((lane1.x2) / widthRatio, lane1.y2 / heightRatio);
            canvas.drawPath(p, paintLane);
        } else if (lane1.departure == 2) {
            Path p = new Path();
            p.moveTo((lane2.x1) / widthRatio, lane2.y1 / heightRatio);
            p.lineTo((lane2.x2) / widthRatio, lane2.y2 / heightRatio);
            canvas.drawPath(p, paintLane);
        }

        float daltX1 = (lane1.x2 - lane1.x1) / 5;
        float daltY1 = (lane1.y2 - lane1.y1) / 5;
        float daltX2 = (lane2.x2 - lane2.x1) / 5;
        float daltY2 = (lane2.y2 - lane2.y1) / 5;

        for (int i = 0; i < 4; i++) {
            Path p = new Path();
            p.moveTo((lane1.x1 + daltX1 * (i + 1)) / widthRatio, (lane1.y1 + daltY1 * (i + 1)) / heightRatio);
            p.lineTo((lane2.x1 + daltX2 * (i + 1)) / widthRatio, (lane2.y1 + daltY2 * (i + 1)) / heightRatio);
            canvas.drawPath(p, paintLane);
        }
    }


    public void clear() {
        carDistances.clear();
        lanes.clear();
    }

}
