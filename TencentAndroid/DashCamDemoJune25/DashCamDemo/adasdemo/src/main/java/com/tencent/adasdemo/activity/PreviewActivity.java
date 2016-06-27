package com.tencent.adasdemo.activity;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.tencent.adasdemo.AdasWrap;
import com.tencent.adasdemo.CameraWrapper;
import com.tencent.adasdemo.Constants;
import com.tencent.adasdemo.CoverView;
import com.tencent.adasdemo.R;

import java.util.Arrays;

public class PreviewActivity extends Activity {

    private static final String TAG = PreviewActivity.class.getSimpleName();

    private TextureView mCameraTexturePreview;

    private AdasWrap mADAS;

    private Handler mHandler = new Handler();

    int screenWidth = Constants.ADAS_WIDTH;
    int screenHeight = Constants.ADAS_HEIGHT;
    float widthRatio = 1280.0f / screenWidth;
    float heightRatio = 720.0f / screenHeight;

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
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
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
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(screenWidth, screenHeight);
        coverView = (CoverView) findViewById(R.id.coverView);
        coverView.setLayoutParams(layoutParams);
        coverView.setRatio(widthRatio, heightRatio);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mADAS == null) {
            mADAS = new AdasWrap(getApplicationContext());
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
            layoutParams = new RelativeLayout.LayoutParams(screenWidth, screenHeight);
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

                    long time = System.currentTimeMillis();

                    if (mADAS != null) {
                        mADAS.detect(source, coverView);
                    }

                    Log.d("ADAS", "costTime: " + (System.currentTimeMillis() - time));

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            coverView.invalidate();
                            CameraWrapper.getInstance().addPreviewBuffer();
                        }
                    });
                }
            }.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraWrapper.getInstance().stopCamera();
        mHandler.removeCallbacks(null);
        if (mADAS != null) {
            mADAS.unInit();
            mADAS = null;
        }
    }
}