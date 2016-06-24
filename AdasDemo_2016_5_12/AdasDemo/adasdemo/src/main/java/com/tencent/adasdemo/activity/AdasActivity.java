package com.tencent.adasdemo.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.tencent.adas.ADASWrapper;
import com.tencent.adas.CarDistance;
import com.tencent.adas.Lane;
import com.tencent.adasdemo.Constants;
import com.tencent.adasdemo.CoverView;
import com.tencent.adasdemo.R;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * 用于本地视频的演示
 */
public class AdasActivity extends Activity implements TextureView.SurfaceTextureListener {

    private Handler mHandler = new Handler();
    private ADASWrapper mADAS;

    private MediaPlayer mMediaPlayer;
    private TextureView mTextureView;

    int viewWidth = Constants.ADAS_WIDTH;
    int viewHeight = Constants.ADAS_HEIGHT;
    float widthRatio = 1920.0f / viewWidth;
    float heightRatio = 1080.0f / viewHeight;
    private RelativeLayout.LayoutParams layoutParams;
    CoverView coverView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_adas);
        init();
    }

    private void init() {
        layoutParams = new RelativeLayout.LayoutParams(viewWidth, viewHeight);
        layoutParams.leftMargin = 0;
        layoutParams.topMargin = 0;

        if (mADAS == null) {
            mADAS = new ADASWrapper();
            mADAS.init(Constants.ADAS_WIDTH, Constants.ADAS_HEIGHT);
        }

        coverView = (CoverView) findViewById(R.id.coverView);
        coverView.setLayoutParams(layoutParams);
        mTextureView = (TextureView) findViewById(R.id.textureView);

        // SurfaceTexture is available only after the TextureView
        // is attached to a window and onAttachedToWindow() has been invoked.
        // We need to use SurfaceTextureListener to be notified when the SurfaceTexture
        // becomes available.
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacks(frameWorker);
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mADAS != null) {
            mADAS.uninit();
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Surface surface = new Surface(surfaceTexture);
        String path = getIntent().getStringExtra("path");
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setLooping(true);

            // don't forget to call MediaPlayer.prepareAsync() method when you use constructor for
            // creating MediaPlayer
            mMediaPlayer.prepareAsync();

            // Play video when the media source is ready for playback.
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {

                    mediaPlayer.start();
                    if (Constants.ADAS_WIDTH == mTextureView.getWidth()) {
                        widthRatio = Constants.ADAS_WIDTH / mTextureView.getWidth();
                    } else {
                        widthRatio = Constants.ADAS_WIDTH * 1.0f / mTextureView.getWidth();
                    }

                    if (Constants.ADAS_HEIGHT == mTextureView.getHeight()) {
                        heightRatio = Constants.ADAS_HEIGHT / mTextureView.getHeight();
                    } else {
                        heightRatio = Constants.ADAS_HEIGHT * 1.0f / mTextureView.getHeight();
                    }
                    mHandler.postDelayed(frameWorker, 100);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private byte[] getNV21(int inputWidth, int inputHeight, Bitmap scaled) {

        int[] argb = new int[inputWidth * inputHeight];

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);

        return yuv;
    }

    private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) & 0xff;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }

                index++;
            }
        }
    }

    private void carDetect(byte[] source, CoverView coverView) {
        long curTime = 0;
        float time = 0;

        curTime = SystemClock.elapsedRealtimeNanos();
        CarDistance distance[] = null;
        distance = mADAS.carDetect(curTime, source);
        time = (SystemClock.elapsedRealtimeNanos() - curTime) / 1000000000.0f;


        if (distance != null && distance.length > 0) {
            boolean debug_Gotconstance = false;

            for (CarDistance item : distance) {
                Log.d("ADAS_CAR", item.toString() + " time: " + time + "s");

                RectF rect = new RectF(item.x / widthRatio, item.y / widthRatio, (item.x + item.width) / widthRatio, (item.y + item.height) / heightRatio);
                coverView.addRect(rect, Integer.toString(item.distance) + "m");

                // TODO: 16/6/9 remove debug code
                float maxH = item.y + item.height;
                if (!debug_Gotconstance && maxH > 0 && maxH <= Constants.ADAS_WIDTH) {
                    debug_Gotconstance = true;
                }
            }

            if (debug_Gotconstance) {
                tone();
            }
        }
    }

    private void tone() {
        int volume = 50;
        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);

            volume = 100 * curVol / maxVol;
        }

        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, volume);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    private void laneDetect(byte[] frame, CoverView coverView) {
        long curTime = 0;
        float time = 0;
        curTime = SystemClock.elapsedRealtimeNanos();
        Lane lane[] = mADAS.LaneDetect(frame, frame.length);
        time = (SystemClock.elapsedRealtimeNanos() - curTime) / 1000000000.0f;


        if (lane != null && lane.length == 2) {
            ArrayList<Path> paths = new ArrayList<>();

            Path path = new Path();
            Log.d("Jeff","drawing the lines");
            /*path.moveTo(lane[0].x1 / widthRatio, lane[0].y1 / heightRatio);
            path.lineTo(lane[0].x2 / widthRatio, lane[0].y2 / heightRatio);
            path.lineTo(lane[1].x2 / widthRatio, lane[1].y2 / heightRatio);
            path.lineTo(lane[1].x1 / widthRatio, lane[1].y1 / heightRatio);
            path.lineTo(lane[0].x1 / widthRatio, lane[0].y1 / heightRatio);
            paths.add(path);*/

            float daltX1 = (lane[0].x2 - lane[0].x1) / 5;
            float daltY1 = (lane[0].y2 - lane[0].y1) / 5;
            float daltX2 = (lane[1].x2 - lane[1].x1) / 5;
            float daltY2 = (lane[1].y2 - lane[1].y1) / 5;

            for (int i = 0; i < 5; i++) {
                Path p = new Path();
                p.moveTo((lane[0].x1 + daltX1 * (i + 1)) / widthRatio, (lane[0].y1 + daltY1 * (i + 1)) / heightRatio);
                p.lineTo((lane[1].x1 + daltX2 * (i + 1)) / widthRatio, (lane[1].y1 + daltY2 * (i + 1)) / heightRatio);
                paths.add(p);
            }

            coverView.addPath(paths);
        }
    }

    Runnable frameWorker = new Runnable() {

        @Override
        public void run() {
            new Thread() {
                @Override
                public void run() {
                    Bitmap source = null;
                    if (mTextureView != null) {
                        source = mTextureView.getBitmap(Constants.ADAS_WIDTH, Constants.ADAS_HEIGHT);
                    }

                    if (source != null) {
                        // 获取 NV21 的数据
                        byte[] data = getNV21(Constants.ADAS_WIDTH, Constants.ADAS_HEIGHT, source);
                        data = Arrays.copyOf(data, Constants.ADAS_WIDTH * Constants.ADAS_HEIGHT);
                        coverView.clear();
                        carDetect(data, coverView);
                        laneDetect(data, coverView);
                    }

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            coverView.invalidate();
                        }
                    });

                    mHandler.post(frameWorker);
                }
            }.start();
        }
    };
}
