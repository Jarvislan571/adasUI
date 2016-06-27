package com.tencent.adasdemo;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SuppressLint("NewApi")
public class CameraWrapper {
    private static final String TAG = "CameraWrapper";
    private Point[] mVideoSize = {new Point(Constants.CameraWidth, Constants.CameraHeight), new Point(1280, 720)};

    private int mCamId = -1;
    private static CameraWrapper mCameraWrapper;
    private Camera mCamera;
    private byte[] mImageCallbackBuffer;
    private Camera.PreviewCallback previewCallback;
    private CamWrapperCallback camWrapperCallback;
    private Point mPreviewSize = new Point();
    private String mFocusMode = null;

    private boolean mIsPreviewing = false;

    private final Object lockCamera = new Object();


    public interface CamWrapperCallback {
        void
        onCameraOpened(Exception e);

        void onCameraStop();
    }

    public static synchronized CameraWrapper getInstance() {
        if (mCameraWrapper == null) {
            mCameraWrapper = new CameraWrapper();
        }
        return mCameraWrapper;
    }

    public void openCamera(CamWrapperCallback callback) {
        Log.i(TAG, "Camera open....");
        Exception err = null;

        synchronized (lockCamera) {
            camWrapperCallback = callback;

            int numCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    try {
                        mCamera = Camera.open(i);
                        if (null != mCamera)
                            mCamId = i;
                        break;
                    } catch (Exception e) {
                        err = e;
                    }
                }
            }
            if (mCamera == null) {
                Log.d(TAG, "No front-facing camera found; opening default");
                try {
                    mCamera = Camera.open();    // opens first back-facing camera
                } catch (Exception e) {
                    if (null == err) {
                        err = e;
                    }
                }
            }
            if (mCamera == null) {
                if (null == err) {
                    err = new RuntimeException("Unable to open camera");
                }
            } else {
                err = null;
            }
        }

        Log.i(TAG, "Camera open over....");
        camWrapperCallback.onCameraOpened(err);
    }

    public void startPreview(SurfaceTexture surface, float previewRate) {
        Log.i(TAG, "doStartPreview()");
        if (mIsPreviewing) {
            this.mCamera.stopPreview();
            return;
        }

        try {
            this.mCamera.setPreviewTexture(surface);
            this.mCamera.startPreview();
            autoFocus(1000);
            mIsPreviewing = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void autoFocus(final int delay) {
        boolean doFocus = null != mFocusMode
                && Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO.compareTo(mFocusMode) != 0;

        if (doFocus) {
            Timer timer = new Timer("autoFocus");

            TimerTask detectTask = new TimerTask() {
                @Override
                public void run() {
                    if (null != mCamera) {
                        try {
                            mCamera.autoFocus(null);
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            timer.schedule(detectTask, delay);
        }
    }

    public void addPreviewBuffer() {
        if (mCamera != null) {
            mCamera.addCallbackBuffer(mImageCallbackBuffer);
        }
    }

    private String selectFocusModeForPreview(Camera.Parameters cameraParameters) {
        String selectMode = null;
        List<String> focusModes = cameraParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            selectMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            selectMode = Camera.Parameters.FOCUS_MODE_AUTO;
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            selectMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        }

        return selectMode;
    }

    private Camera.Size selectSizeForPreview(Camera.Parameters cameraParameters) {
        Camera.Size previewSize = null;

        List<Camera.Size> supportedSizes = cameraParameters.getSupportedPreviewSizes();
        for (int i = 0; i < supportedSizes.size() && (null == previewSize); ++i) {
            Camera.Size size = supportedSizes.get(i);
            for (Point preset : mVideoSize) {
                if (size.width == preset.x && size.height == preset.y) {
                    previewSize = size;
                    break;
                }
            }
        }

        if (null == previewSize) {
            previewSize = cameraParameters.getPreferredPreviewSizeForVideo();
        }

        return previewSize;
    }

    public void initCamera(int width, int height, Camera.PreviewCallback callback) {
        if (this.mCamera != null) {
            previewCallback = callback;
            Camera.Parameters mCameraParamters;
            mCameraParamters = this.mCamera.getParameters();
            mCameraParamters.setPreviewFormat(ImageFormat.NV21);
            mCameraParamters.setFlashMode("off");
            mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            //this.mCamera.setDisplayOrientation(90);

            mCamera.setPreviewCallbackWithBuffer(previewCallback);

            Camera.Size previewSize = selectSizeForPreview(mCameraParamters);
            if (null != previewSize) {
                mPreviewSize.x = previewSize.width;
                mPreviewSize.y = previewSize.height;
                mCameraParamters.setPreviewSize(previewSize.width, previewSize.height);
            } else {
                mPreviewSize.x = width;
                mPreviewSize.y = height;
                mCameraParamters.setPreviewSize(width, height);
            }

            mFocusMode = selectFocusModeForPreview(mCameraParamters);
            if (null != mFocusMode) {
                mCameraParamters.setFocusMode(mFocusMode);
            }

            mImageCallbackBuffer = new byte[mPreviewSize.x * mPreviewSize.y * 3 / 2];
            //mCamera.addCallbackBuffer(mImageCallbackBuffer);

            this.mCamera.setParameters(mCameraParamters);
        }
    }

    public void stopCamera() {
        Log.i(TAG, "doStopCamera");
        synchronized (lockCamera) {
            if (this.mCamera != null) {
                camWrapperCallback.onCameraStop();
                this.mCamera.setPreviewCallback(null);
                this.mCamera.stopPreview();
                this.mIsPreviewing = false;
                this.mCamera.release();
                this.mCamera = null;
            }
        }
    }

    public Point getPreviewSize() {
        return mPreviewSize;
    }

    private SurfaceHolder mHolder = null;
    private boolean bRecording = false;

    //  region  video record
    public void previewRecord(int width, int height, SurfaceHolder holder) {
        if (this.mCamera != null) {
            mHolder = holder;
            Camera.Parameters mCameraParamters;
            mCameraParamters = this.mCamera.getParameters();
            mCameraParamters.setPreviewFormat(ImageFormat.NV21);
            mCameraParamters.setFlashMode("off");
            mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

            mCameraParamters.setPreviewSize(width, height);

            Camera.Size previewSize = selectSizeForPreview(mCameraParamters);
            if (null != previewSize) {
                mPreviewSize.x = previewSize.width;
                mPreviewSize.y = previewSize.height;
                mCameraParamters.setPreviewSize(previewSize.width, previewSize.height);
            } else {
                mPreviewSize.x = width;
                mPreviewSize.y = height;
                mCameraParamters.setPreviewSize(width, height);
            }

            mFocusMode = selectFocusModeForPreview(mCameraParamters);
            if (null != mFocusMode) {
                mCameraParamters.setFocusMode(mFocusMode);
            }

            mCamera.setParameters(mCameraParamters);
            mCamera.setDisplayOrientation(0);
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                autoFocus(1000);
            } catch (IOException e) {
            }
        }
    }

    MediaRecorder mMediaRecorder = null;

    public void startRecord(String videoFile) {
        synchronized (lockCamera) {
            if (null != mCamera) {
                if (bRecording == false) {
                    bRecording = true;

                    //String videoFile = "/mnt/extsd/Tencent/video_sample.mp4";
                    File f = new File(videoFile);
                    if (f.exists()) {
                        f.delete();
                    }

                    if (mCamera == null) {
                        return;
                    }

                    try {
                        mCamera.unlock();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                    mMediaRecorder = new MediaRecorder();// 创建mediarecorder对象
                    // 设置录制视频源为Camera(相机)
                    mMediaRecorder.setCamera(mCamera);
                    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);

                    //			mediarecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));

                    // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
                    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    // 设置录制的视频编码h263 h264
                    //mediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                    //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
                    // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
                    mMediaRecorder.setVideoSize(mPreviewSize.x, mPreviewSize.y);
                    //mediarecorder.setVideoEncodingBitRate(bitRat);
                    // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
                    mMediaRecorder.setVideoFrameRate(30);
                    mMediaRecorder.setVideoEncodingBitRate(3000000);
                    //			mediarecorder.setOrientationHint(90);
                    //			mediarecorder.setPreviewDisplay(surfaceview.getHolder().getSurface());
                    // 设置视频文件输出的路径
                    mMediaRecorder.setOutputFile(videoFile);

                    try {
                        mMediaRecorder.prepare();
                        mMediaRecorder.start();

                        Log.i(TAG, "recording...");
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        bRecording = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                        bRecording = false;
                    }
                }
            }
        }
    }

    public void stopRecordVideo() {
        try {
            if (mMediaRecorder != null) {
                if (bRecording) {
                    mMediaRecorder.stop();
                    Log.i(TAG, "stop record...");
                }
                mMediaRecorder.release();
                mMediaRecorder = null;
            }

            mCamera.lock();
            bRecording = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  endregion  video record
}
