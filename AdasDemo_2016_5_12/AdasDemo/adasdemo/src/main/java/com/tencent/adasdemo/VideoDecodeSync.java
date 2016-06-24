package com.tencent.adasdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;

import com.tencent.adas.ADASWrapper;
import com.tencent.adas.CarDistance;
import com.tencent.adas.Lane;

import java.io.IOException;
import java.nio.ByteBuffer;


public class VideoDecodeSync implements Runnable {

    private static final String TAG = "ADAS";
    private static final boolean VERBOSE = true;
    private final Canvas canvas;
    private final Paint paint;
    private final Paint paint_txt;
    private Object decodeScoreTaskObject = new Object();
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
    private long currentTimeMs;
    private int uvOffset;
    private int stride;
    private ADASWrapper mADAS;

    public VideoDecodeSync(Context context, Uri videoUri, Bitmap outputBitmap, ImageView mImageView, int inputWidth, int inputHeight, MediaFormat format, MediaExtractor extractor, String mime) {

        this.context = context;
        this.videoUri = videoUri;

        this.outputBitmap = outputBitmap;
        this.mImageView = mImageView;
        Log.d("kneron", "inputWidth: " + inputWidth + " inputHeight: " + inputHeight);

        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
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
    }


    @Override
    public void run() {

        final int TIMEOUT_USEC = 10000;


        mADAS.init(inputWidth, inputHeight);

        try {
            codec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }

        codec.configure(format, null, null, 0);
        codec.start();

        BufferInfo info = new BufferInfo();

        boolean inputDone = false;
        boolean decoderDone = false;

        while (!decoderDone) {

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
                        if (VERBOSE) Log.d(TAG, "decoder input: queued frame for sampleTime = " + sampleTime);
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
                    if (VERBOSE) Log.d(TAG, "decoder output: format changed: " + codec.getOutputFormat());
                } else if (decoderOutputStatusOrIndex >= 0) {

                    if (VERBOSE) Log.d(TAG, "decoder output: frame available: sampleTime = " + info.presentationTimeUs);

                    decoderOutputAvailable = true;
                    ByteBuffer outputFrame = codec.getOutputBuffer(decoderOutputStatusOrIndex);

                    if (stride == 0 && info.size != 0) //not found yet
                    {
                        MediaFormat format = codec.getOutputFormat();
                        stride = (format.containsKey("stride") ?
                                format.getInteger("stride") : inputWidth);
                        uvOffset = (format.containsKey("slice-height") ?
                                format.getInteger("slice-height") : inputHeight) * stride;
                    }

                    if (info.size != 0) {

                        long newTimeMs = System.currentTimeMillis();
                        //Log.d(TAG, "[" + (newTimeMs - currentTimeMs) + " ms from previous output frame] frame count = " + outputFrameCnt);
                        outputFrameCnt++;
                        long processTimeMsEnd,processTimeMs;

                        byte[] data_arr = new byte[outputFrame.remaining()];
                        Log.d("kneron_car","the size of input data " + outputFrame.remaining() );
                        outputFrame.get(data_arr);
                        // car detection

                        long curTime = SystemClock.elapsedRealtimeNanos();
                        CarDistance distance[] = null;
                        distance    = mADAS.carDetect(curTime, data_arr);
                        processTimeMsEnd = System.currentTimeMillis();
                        processTimeMs = processTimeMsEnd - currentTimeMs;
                        Log.d("CAR_TIME", "processTimeMs: " + processTimeMs + " ms");
                        int number_bounding_box = distance.length;
//                        int[] bounding_box = mCar.getBoundingBox();
                        Log.d("~~~~~CAR_TIME", " # of cars detected " + number_bounding_box );


                        // lane detection
                        Lane lane[] = null;
                        currentTimeMs = System.currentTimeMillis();
                        lane = mADAS.LaneDetect(data_arr, data_arr.length);
                        processTimeMsEnd = System.currentTimeMillis();
                        processTimeMs = processTimeMsEnd - currentTimeMs;
                        Log.d("LANE_TIME", "processTimeMs: " + processTimeMs + " ms");
                        int number_Lines = lane.length;
//                        int[] Lines = mLane.getLineBoundingBox();


//                        //for display test
//                        mCar.YUV420ByteBufferToRGB8888Bitmap(outputFrame, uvOffset, inputWidth, inputHeight, stride, stride/2, outputBitmap);
//                        codec.releaseOutputBuffer(decoderOutputStatusOrIndex, false);
//
//                        for (int i = 0; i < number_bounding_box; i++) {
//                            int leftEdge = bounding_box[i * 5 + 0];
//                            int topEdge = bounding_box[i * 5 + 1];
//                            int width = bounding_box[i * 5 + 2];
//                            int height = bounding_box[i * 5 + 3];
//                            int distance = bounding_box[i * 5 + 4];
//                            Log.d(TAG,  "x = " + leftEdge + " y = " + topEdge + " width = " + width + "height =" + height);
//                            canvas.drawRect(leftEdge, topEdge, leftEdge + width, topEdge + height, paint);
//                            canvas.drawText(String.valueOf(distance)+"m", leftEdge + 20, topEdge + 20, paint_txt);
//                        }
//
//                        if(number_Lines == 2) {
//                                int x1 = Lines[0];
//                                int y1 = Lines[1];
//                                int x2 = Lines[2];
//                                int y2 = Lines[3];
//                                int x3 = Lines[4];
//                                int y3 = Lines[5];
//                                int x4 = Lines[6];
//                                int y4 = Lines[7];
//                                int delta_ydist = (y2-y1)/10;
//                                int xdist_one_delta = (x1 - x2)/10;
//                                int xdist_two_delta = (x4 - x3)/10;
//                                for(int i = 0 ;i < 10;i++)
//                                    canvas.drawLine(x2+xdist_one_delta*i, y2-delta_ydist*i, x4-xdist_two_delta*i, y4-delta_ydist*i, paint_txt);
//                        }
                        //Log.d(TAG, "detecting the bounding box " + number_bounding_box);
                        mImageView.postInvalidate();
                    }

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) Log.d(TAG, "decoder got EOS");
                        decoderOutputAvailable = false;
                        releaseDecodingResources();
                        mADAS.uninit();
                        decoderDone = true;
                    }
                }
            }
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

}
