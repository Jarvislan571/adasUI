package com.tencent.adasdemo.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.tencent.adasdemo.R;
import com.tencent.adasdemo.VideoDecodeSync;

import java.io.File;
import java.io.IOException;


/**
 * 用于本地视频的演示
 */
public class AdasActivity extends Activity {

    ImageView mImageView;
    VideoDecodeSync videoDecodeScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_adas);
        mImageView = (ImageView) findViewById(R.id.imageView);
        String path = getIntent().getStringExtra("path");
        setupDecode(path);
    }

    private void setupDecode(String fileName) {

        //String mp4FilePath = Environment.getExternalStorageDirectory() + fileName;

        Log.d("Kneron", " getExternalStorageDirectory " + fileName);
        Uri videoUri = Uri.fromFile(new File(fileName));

        /* Initialize and set the datasource of the media extractor */
        MediaExtractor extractor = new MediaExtractor();

        try {
            extractor.setDataSource(this, videoUri, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * Iterate over the available tracks and choose the video track. Choose
		 * the codec by type and configure the codec
		 */
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);

            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {

                extractor.selectTrack(i);
                int inputWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                int inputHeight = format.getInteger(MediaFormat.KEY_HEIGHT);

                Bitmap outputBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888);
                mImageView.setImageBitmap(outputBitmap);

                //format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

                Log.d("ADAS", "Found a video track afdas.");

                videoDecodeScore = new VideoDecodeSync(this, videoUri, outputBitmap, mImageView, inputWidth, inputHeight, format, extractor, mime);
                Thread decodeScore = new Thread(videoDecodeScore);
                decodeScore.start();
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoDecodeScore != null) {
            videoDecodeScore.stop();
        }

    }
}
