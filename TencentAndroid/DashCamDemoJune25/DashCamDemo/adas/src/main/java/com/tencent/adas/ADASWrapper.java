package com.tencent.adas;


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

    final int maxResult   = 128;
    CarDistance[] mTempCarDistance = new CarDistance[maxResult];
    Lane[] mTempLane = new Lane[maxResult];

    public native int init(int width, int height, float side, float upper, float lower);
    public native int uninit();

    public native int carDetectNative(CarDistance[] result, long elapsedRealtimeNanos, byte[] frame);
    public native int LaneDetectNative(Lane[] result, long elapsedRealtimeNanos, byte[] frame);

    public CarDistance[] carDetect(long elapsedRealtimeNanos, byte[] frame) {
        CarDistance[]   result  = null;
        int count   = carDetectNative(mTempCarDistance, elapsedRealtimeNanos, frame);
        if (count > 0) {
            result  = new CarDistance[count];
            java.lang.System.arraycopy(mTempCarDistance, 0, result, 0, count);
        }
        return  result;
    };
    public Lane[] LaneDetect(byte[] frame, int bufLen) {
        Lane[]   result  = null;
        int count   = LaneDetectNative(mTempLane, 0, frame);

        if (count > 0) {
            result  = new Lane[count];
            java.lang.System.arraycopy(mTempLane, 0, result, 0, count);
        }

        return result;
    }
}
