package com.tencent.adasdemo.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.tencent.adas.ADASWrapper;
import com.tencent.adas.CarDistance;
import com.tencent.adas.Lane;
import com.tencent.adasdemo.CameraWrapper;
import com.tencent.adasdemo.Constants;
import com.tencent.adasdemo.CoverView;
import com.tencent.adasdemo.R;

import java.util.ArrayList;
import java.util.Arrays;

public class PreviewActivity extends Activity {

    private static final String TAG = PreviewActivity.class.getSimpleName();

    private TextureView mCameraTexturePreview;
    private RelativeLayout mContainer;

    private ADASWrapper mADAS;

    private Handler mHandler = new Handler();

    int viewWidth = Constants.ADAS_WIDTH;
    int viewHeight = Constants.ADAS_HEIGHT;
    float widthRatio = 1920.0f / viewWidth;
    float heightRatio = 1080.0f / viewHeight;

    CoverView coverView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_preview);
        TextureListener mTextureListener = new TextureListener();

        mCameraTexturePreview = (TextureView) findViewById(R.id.preview);
        mCameraTexturePreview.setSurfaceTextureListener(mTextureListener);

        ViewGroup.LayoutParams params = mCameraTexturePreview.getLayoutParams();
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        params.width = screenWidth;
        params.height = screenHeight;
        mCameraTexturePreview.setLayoutParams(params);
        if (Constants.CameraHeight == screenHeight) {
            heightRatio = Constants.CameraHeight / screenHeight;
        } else {
            heightRatio = Constants.CameraHeight * 1.0f / screenHeight;
        }

        if (Constants.CameraWidth == screenWidth) {
            widthRatio = Constants.CameraWidth / screenWidth;
        } else {
            widthRatio = Constants.CameraWidth * 1.0f / screenWidth;
        }
        mContainer = (RelativeLayout) findViewById(R.id.preview_container);
        coverView = (CoverView) findViewById(R.id.coverView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mADAS == null) {
            mADAS = new ADASWrapper();
            mADAS.init(Constants.CameraWidth, Constants.CameraHeight);
        }
    }

    private class TextureListener implements TextureView.SurfaceTextureListener {
        private Thread mThread = null;

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                              int height) {
            Log.i(TAG, "onSurfaceTextureAvailable()");

            mThread = new Thread() {
                @Override
                public void run() {
                    CameraWrapper.getInstance().openCamera(new CamWrapperCallback());
                }
            };
            mThread.start();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                                int height) {
            Log.i(TAG, "onSurfaceTextureSizeChanged()");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.i(TAG, "onSurfaceTextureDestroyed()");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }

    private class CamWrapperCallback implements CameraWrapper.CamWrapperCallback {
        public void onCameraOpened(Exception err) {
            if (null == err) {
                SurfaceTexture surface = mCameraTexturePreview.getSurfaceTexture();

                CameraWrapper.getInstance().initCamera(Constants.CameraWidth, Constants.CameraHeight, new CameraPreviewCallback());
                CameraWrapper.getInstance().startPreview(surface, 1);
                schedulePreview();
            } else {
                err.printStackTrace();
            }
        }

        public void onCameraStop() {
        }
    }

    private void schedulePreview() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, Thread.currentThread().getName());
                CameraWrapper.getInstance().addPreviewBuffer();
            }
        });
    }


    private class CameraPreviewCallback implements android.hardware.Camera.PreviewCallback {
        RelativeLayout.LayoutParams layoutParams;

        public CameraPreviewCallback() {
            layoutParams = new RelativeLayout.LayoutParams(viewWidth, viewHeight);
            layoutParams.leftMargin = 0;
            layoutParams.topMargin = 0;
        }

        @Override
        public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
            Log.d(TAG, "onPreviewFrame" + Thread.currentThread().getName());

            final byte[] source = Arrays.copyOf(data, Constants.CameraWidth * Constants.CameraHeight);
            new Thread() {
                @Override
                public void run() {
                    coverView.clear();
                    //debug purpose assuming the input is 1080p.
                    RectF rect = new RectF(480 / widthRatio, 756/ widthRatio,1478 / widthRatio, 1080/ heightRatio);
                    coverView.addRect(rect);

                    rect = new RectF(576 / widthRatio, 540/ widthRatio,1344 / widthRatio, 1080/ heightRatio);
                    coverView.addRect(rect);
                    carDetect(source, coverView);
                    laneDetect(source, coverView);


                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            coverView.setLayoutParams(layoutParams);
                            coverView.invalidate();
                            CameraWrapper.getInstance().addPreviewBuffer();
                        }
                    });
                }
            }.start();
        }

        void carDetect(byte[] source, CoverView coverView) {
            long curTime = 0;
            float time = 0;

            curTime = SystemClock.elapsedRealtimeNanos();
            CarDistance distance[] = null;
            if (mADAS != null) {
                distance = mADAS.carDetect(curTime, source);
            }

            time = (SystemClock.elapsedRealtimeNanos() - curTime) / 1000000000.0f;


            if (distance != null && distance.length > 0) {
                boolean debug_Gotconstance = false;

                for (CarDistance item : distance) {
                    Log.d("ADAS", item.toString() + " time: " + time + "s");

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

        void tone() {
            int volume = 50;
            AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_RING);
                int curVol = am.getStreamVolume(AudioManager.STREAM_RING);

                volume = 100 * curVol / maxVol;
            }

            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, volume);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        }

        void laneDetect(byte[] frame, CoverView coverView) {
            long curTime = 0;
            float time = 0;
            curTime = SystemClock.elapsedRealtimeNanos();
            Lane lane[] = null;
            if (mADAS != null) {
                lane = mADAS.LaneDetect(frame, frame.length);
            }

            time = (SystemClock.elapsedRealtimeNanos() - curTime) / 1000000000.0f;

            if (lane != null && lane.length == 2) {
                ArrayList<Path> paths = new ArrayList<>();

                Path path = new Path();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraWrapper.getInstance().stopCamera();
        mHandler.removeCallbacks(null);
        if (mADAS != null) {
            mADAS.uninit();
            mADAS = null;
        }
    }
}