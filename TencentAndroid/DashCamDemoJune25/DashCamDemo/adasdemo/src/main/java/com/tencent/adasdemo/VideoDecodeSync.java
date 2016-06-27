package com.tencent.adasdemo;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.tencent.adas.ADASWrapper;
import com.tencent.adas.CarDistance;
import com.tencent.adas.Lane;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;

public class VideoDecodeSync implements Runnable {

    private static final String TAG = "ADAS";
    private static final boolean VERBOSE = true;
    private static final int marginRight = 10;
    private static final int marginTop = 8;

    private static final int rectWidth = 200;
    private static final int rectHeight = 200;

    private Canvas canvas;
    private final Paint paint;
    private final Paint paintRegion;
    private final Paint paint_txt;
    private final Paint paintRect;
    private final Paint paintDesc;
    private final Paint paintTip;
    private final Paint paintLane;

    float widthRatio;
    float heightRatio;

    /* Pulls encoded AV from source */
    private MediaExtractor extractor;
    /* Used in this case to decode video frame returned by the MediaExtractor */
    private MediaCodec codec;
    /* Path to the encoded AV source */
    private Uri videoUri;
    private Context context;
    private ImageView mImageView;
    private int inputWidth;
    private int inputHeight;
    private MediaFormat format;
    private String mime;
    private int outputFrameCnt;
    private Bitmap outputBitmap;
    private long currentTimeNano;
    private int uvOffset;
    private int stride;
    private ADASWrapper mADAS;
    private boolean isRunning = true;


    private long lastAdviceTime = System.currentTimeMillis();
    private long lastAlertTime = System.currentTimeMillis();

    private int imageIndex = 1;

    public VideoDecodeSync(Context context, Uri videoUri, Bitmap outputBitmap, ImageView mImageView, int inputWidth, int inputHeight, MediaFormat format, MediaExtractor extractor, String mime) {

        this.context = context;
        this.videoUri = videoUri;

        this.outputBitmap = outputBitmap;
        this.mImageView = mImageView;
        Log.d("kneron", "inputWidth: " + inputWidth + " inputHeight: " + inputHeight);

        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
        this.widthRatio = (float) inputWidth / 1920;
        this.heightRatio = (float) inputHeight / 1080;
        this.format = format;
        this.mime = mime;
        this.extractor = extractor;

        canvas = new Canvas(this.outputBitmap);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(255, 0, 0));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        paint_txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint_txt.setColor(Color.rgb(0, 255, 0));
        paint_txt.setStyle(Paint.Style.STROKE);
        paint_txt.setStrokeWidth(2);
        paint_txt.setTextSize(30);


        paintRegion = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRegion.setColor(Color.rgb(0, 255, 255));
        paintRegion.setStyle(Paint.Style.STROKE);
        paintRegion.setStrokeWidth(8);

        paintRect = new Paint(Paint.ANTI_ALIAS_FLAG);

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

        paintLane = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLane.setColor(Color.GREEN);
        paintLane.setStyle(Paint.Style.STROKE);//实线
        paintLane.setStrokeWidth(2);//线宽

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {

        final int TIMEOUT_USEC = 10000;

        Log.d("ADAS2","initialization");
        mADAS = new ADASWrapper();
        mADAS.init(inputWidth, inputHeight, 0.3f, 0.4f, 0.2f);

        try {
            codec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }

        codec.configure(format, null, null, 0);
        codec.start();

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        boolean inputDone = false;
        boolean decoderDone = false;

        while (!decoderDone && isRunning) {

            ////////////////////////////////
            // Feed more data to the decoder.
            ////////////////////////////////
            if (!inputDone) {

                int decoderInputStatusOrIndex = codec.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputStatusOrIndex >= 0) {

                    ByteBuffer buffer = codec.getInputBuffer(decoderInputStatusOrIndex);
                    int sampleSize = extractor.readSampleData(buffer, 0);

                    if (sampleSize > 0) {
                        long sampleTime = extractor.getSampleTime();
                        codec.queueInputBuffer(decoderInputStatusOrIndex, 0, sampleSize,
                                sampleTime, 0);
                        if (VERBOSE)
                            Log.d(TAG, "decoder input: queued frame for sampleTime = " + sampleTime);
                        extractor.advance();
                    } else {
                        // End of stream -- send empty frame with EOS flag set.
                        codec.queueInputBuffer(decoderInputStatusOrIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        if (VERBOSE) Log.d(TAG, "sent input EOS (with zero-length frame)");
                    }
                } else {
                    if (VERBOSE) Log.d(TAG, "input buffer not available");
                }
            }

            boolean decoderOutputAvailable = true;
            while (decoderOutputAvailable) {

                int decoderOutputStatusOrIndex = codec.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderOutputStatusOrIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "decoder output: no output available");
                    decoderOutputAvailable = false;
                } else if (decoderOutputStatusOrIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // expected before first buffer of data
                    if (VERBOSE)
                        Log.d(TAG, "decoder output: format changed: " + codec.getOutputFormat());
                } else if (decoderOutputStatusOrIndex >= 0) {

                    if (VERBOSE)
                        Log.d(TAG, "decoder output: frame available: sampleTime = " + info.presentationTimeUs);

                    decoderOutputAvailable = true;
                    ByteBuffer outputFrame = codec.getOutputBuffer(decoderOutputStatusOrIndex);
                    //Image image = codec.getOutputImage(decoderOutputStatusOrIndex);

                    if (stride == 0 && info.size != 0) //not found yet
                    {
                        MediaFormat format = codec.getOutputFormat();
                        stride = (format.containsKey("stride") ?
                                format.getInteger("stride") : inputWidth);
                        uvOffset = (format.containsKey("slice-height") ?
                                format.getInteger("slice-height") : inputHeight) * stride;
                    }

                    if (info.size != 0) {

                        outputFrameCnt++;

                        byte[] data_arr = new byte[outputFrame.remaining()];
                        outputFrame.get(data_arr);
                        currentTimeNano = System.nanoTime();
                        long time = System.currentTimeMillis();

//                        ByteBuffer bufferY = image.getPlanes()[0].getBuffer();
//                        ByteBuffer bufferUV = image.getPlanes()[2].getBuffer();
//
//                        byte[] data = new byte[bufferY.remaining() + bufferUV.remaining() + 1];
//                        int len = inputHeight * inputWidth;
//                        bufferY.get(data, 0, len);
//                        bufferUV.get(data, len, bufferUV.remaining());

//                        if (imageIndex == 2) {
//                            try {
//                                FileOutputStream output = new FileOutputStream("/sdcard/raw");
//                                output.write(data_arr);
//                                output.flush();
//                                output.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
                        CarDistance[] carDistances = mADAS.carDetect(currentTimeNano, data_arr);
                        Lane[] lanes = mADAS.LaneDetect(data_arr, data_arr.length);
                        Log.d("ADAS", "costtime:" + (System.currentTimeMillis() - time));
                        for(int i = 0; i<lanes.length;i++)
                        Log.d("ADAS2","line positionsl " + lanes[i].toString());

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        YuvImage yuvImage = new YuvImage(data_arr, ImageFormat.NV21, inputWidth, inputHeight, null);
                        yuvImage.compressToJpeg(new Rect(0, 0, inputWidth, inputHeight), 100, out);
                        byte[] imageBytes = out.toByteArray();

                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                        codec.releaseOutputBuffer(decoderOutputStatusOrIndex, false);

                        // 敏感区域
                        canvas = new Canvas(outputBitmap);
                        // 车道
                        drawLaneArea(canvas, 480, 756, 1478, 1080);
                        //drawLaneArea(canvas, 1920 * 0.3f, 1080 * 0.67f, 1920 * 0.73f, 1080 * 0.84f);
                        // 车辆
                        drawLaneArea(canvas, 1920 * 0.3f, 1080 * 0.4f, 1920 * 0.7f, 1080 * 0.8f);

                        // 车辆检测结果
                        if (carDistances != null) {
                            for (int i = 0; i < carDistances.length; i++) {
                                drawCarDistance(carDistances[i], canvas, i);
                            }
                        }

                        if (lanes != null && lanes.length == 2) {
                            drawLanes(lanes, canvas);
                        }

                        //saveImage(outputBitmap, imageIndex);
                        imageIndex++;

                        mImageView.post(new Runnable() {
                            @Override
                            public void run() {
                                mImageView.setImageBitmap(outputBitmap);
                                mImageView.invalidate();
                            }
                        });
                    }

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 && isRunning) {
                        if (VERBOSE) Log.d(TAG, "decoder got EOS");
                        decoderOutputAvailable = false;
                        releaseDecodingResources();
                        //mADAS.uninit();
                        //Log.d("ADAS2","uninit");
                        decoderDone = true;
                    }

                    if(!isRunning)
                    {
                        mADAS.uninit();
                        Log.d("ADAS2","uninit");
                    }
                }
            }
        }
    }

    public void drawLaneArea(Canvas canvas, float left, float top, float right, float bottom) {

        RectF rect = new RectF();

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

    // 画矩形图，以及提示信息
    public void drawCarDistance(CarDistance carDistance, Canvas canvas, int index) {
        String tip = null;
        if (carDistance.distance > 20) {
            paintRect.setColor(Color.WHITE);
        } else if (carDistance.distance > 10) {
            paintRect.setColor(Color.YELLOW);
            paintTip.setColor(Color.YELLOW);
            tip = "建议安全距离150米";
            advice();
        } else {
            paintRect.setColor(Color.RED);
            paintTip.setColor(Color.RED);
            tip = "刹车";
            alert();
        }

        RectF carRect = new RectF(carDistance.x,
                carDistance.y,
                (carDistance.x + carDistance.width),
                (carDistance.y + carDistance.height)
        );

        int top = (marginTop + (marginTop + rectHeight) * index);
        int left = (inputWidth - marginRight - rectWidth);
        int right = (inputWidth - marginRight);
        int bottom = (marginTop + rectHeight + (marginTop + rectHeight) * index);
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
        canvas.drawText(carDistance.distance + "米", descRect.centerX(), baseline, paintDesc);

        // 第一行 文字”车辆“
        paintDesc.setTextSize(40);
        fontMetrics = paintDesc.getFontMetricsInt();
        baseline = (int) ((descRect.top + descRect.top + descRect.height() / 3)
                - (fontMetrics.bottom + fontMetrics.top)) / 2;
        canvas.drawText("车辆", descRect.centerX(), baseline, paintDesc);

        // 第三行 TTC
        if (carDistance.timeToCrash > 0 && carDistance.timeToCrash < 20) {
            DecimalFormat format = new DecimalFormat("##0.00");
            canvas.drawText(format.format(carDistance.timeToCrash) + "s", descRect.centerX(), descRect.bottom - 20, paintDesc);
        }

        fontMetrics = paintTip.getFontMetricsInt();
        if (!TextUtils.isEmpty(tip)) {
            canvas.drawText(tip, inputWidth / 2, 50 + (50 + fontMetrics.bottom - fontMetrics.top) * index, paintTip);
        }
    }

    private void advice() {
        long currTime = System.currentTimeMillis();
        if ((currTime - lastAdviceTime) > 3000) {
            lastAdviceTime = currTime;
            tone();
        }
    }

    private void alert() {
        long currTime = System.currentTimeMillis();
        if ((currTime - lastAlertTime) > 1000) {
            lastAlertTime = currTime;
            tone();
        }
    }


    private void tone() {
        int volume = 50;
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_RING);
            int curVol = am.getStreamVolume(AudioManager.STREAM_RING);

            volume = 100 * curVol / maxVol;
        }

        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, volume);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    /**
     * 画车道
     */
    private void drawLanes(Lane[] lanes, Canvas canvas) {
        paintLane.setColor(Color.GREEN);
        Path path = new Path();
        path.moveTo(lanes[0].x1, lanes[0].y1);
        path.lineTo(lanes[0].x2, lanes[0].y2);
        path.lineTo(lanes[1].x2, lanes[1].y2);
        path.lineTo(lanes[1].x1, lanes[1].y1);
        path.lineTo(lanes[0].x1, lanes[0].y1);
        canvas.drawPath(path, paintLane);

        float daltX1 = (lanes[0].x2 - lanes[0].x1) / 5;
        float daltY1 = (lanes[0].y2 - lanes[0].y1) / 5;
        float daltX2 = (lanes[1].x2 - lanes[1].x1) / 5;
        float daltY2 = (lanes[1].y2 - lanes[1].y1) / 5;

        for (int i = 0; i < 4; i++) {
            Path p = new Path();
            p.moveTo((lanes[0].x1 + daltX1 * (i + 1)), (lanes[0].y1 + daltY1 * (i + 1)));
            p.lineTo((lanes[1].x1 + daltX2 * (i + 1)), (lanes[1].y1 + daltY2 * (i + 1)));
            canvas.drawPath(p, paintLane);
        }

        if (lanes[0].departure == Lane.ON_LEFT_LINE) {
            paintLane.setColor(Color.RED);
            Path p = new Path();
            p.moveTo((lanes[0].x1), lanes[0].y1);
            p.lineTo((lanes[0].x2), lanes[0].y2);
            canvas.drawPath(p, paintLane);
            App.speak("left");
        } else if (lanes[0].departure == Lane.ON_RIGHT_LINE) {
            paintLane.setColor(Color.RED);
            Path p = new Path();
            p.moveTo((lanes[1].x1), lanes[1].y1);
            p.lineTo((lanes[1].x2), lanes[1].y2);
            canvas.drawPath(p, paintLane);
            App.speak("汽车向右偏移，请注意");
        }

    }


    private void releaseDecodingResources() {

        if (codec != null) {
            codec.stop();
            codec.release();
            codec = null;
        }
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }

        Log.d(TAG, "Task released resources.");
    }


    private void saveImage(Bitmap bitmap, int index) {
        StringBuilder fileName = new StringBuilder();
        if (index < 10) {
            fileName.append("000").append(index).append(".jpg");
        } else if (index < 100) {
            fileName.append("00").append(index).append(".jpg");
        } else if (index < 1000) {
            fileName.append("0").append(index).append(".jpg");
        } else {
            fileName.append(index).append(".jpg");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

        try {
            FileOutputStream output = new FileOutputStream("/sdcard/outputs/" + fileName.toString());
            output.write(bos.toByteArray());
            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
    }

}
