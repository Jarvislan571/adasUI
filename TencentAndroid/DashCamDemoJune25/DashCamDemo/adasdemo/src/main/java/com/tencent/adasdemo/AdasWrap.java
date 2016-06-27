package com.tencent.adasdemo;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;

import com.tencent.adas.ADASWrapper;
import com.tencent.adas.CarDistance;
import com.tencent.adas.Lane;

/**
 * @author xiaojunzhou
 * @date 16/6/20
 */
public class AdasWrap {

    private ADASWrapper mADAS;
    private Context context;
    private float side = 0.3f;
    private float upper = 0.4f;
    private float lower = 0.2f;

    public AdasWrap(Context context) {
        this.context = context;
    }

    public void init(int width, int height) {
        mADAS = new ADASWrapper();
        mADAS.init(width, height, side, upper, lower);
    }

    public void unInit() {
        if (mADAS != null) {
            mADAS.uninit();
            mADAS = null;
        }
    }


    public void detect(byte[] frame, CoverView coverView) {
        if (frame != null && coverView != null) {
            carDetect(frame, coverView);
            laneDetect(frame, coverView);
        }
    }

    private void carDetect(byte[] source, CoverView coverView) {
        long curTime = SystemClock.elapsedRealtimeNanos();
        CarDistance distance[] = null;
        if (mADAS != null) {
            distance = mADAS.carDetect(curTime, source);
        }

        if (distance != null && distance.length > 0) {
            for (CarDistance item : distance) {
                coverView.addCarDistance(item);
            }
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

    private void laneDetect(byte[] frame, CoverView coverView) {
        if (mADAS != null) {
            Lane[] lane = mADAS.LaneDetect(frame, frame.length);
            if (lane != null && lane.length == 2) {
                for (int i = 0; i < lane.length; i++) {
                    coverView.addLane(lane[i]);
                }
            }
        }
    }
}
