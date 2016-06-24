package com.tencent.adas;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ADASWrapper {
    static String TAG = "TXADASService";

    static {
        try {
            System.loadLibrary("kneron_adas_car");
            System.loadLibrary("kneron_adas_lane");

            System.loadLibrary("TXADASService");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public native int init(int width, int height);
    public native int uninit();

    public native CarDistance[] carDetect(long elapsedRealtimeNanos, byte[] frame);

    public native Lane[] LaneDetect(byte[] frame, int bufLen);
}
