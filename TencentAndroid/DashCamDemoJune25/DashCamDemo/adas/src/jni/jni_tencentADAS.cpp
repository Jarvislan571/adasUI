#include <stdlib.h>
#include <jni.h>
#include <time.h>
#include <android/log.h>
#include "adas.h"
#include "LaneDetector.h"

#ifdef __cplusplus
extern "C" {
#endif


int maxResult   = 128;
CarDistance *g_distance   = NULL;

Lane *g_lanes   = NULL;



/*
 * Class:     ADASWrapper
 * Method:    init
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jint JNICALL Java_com_tencent_adas_ADASWrapper_init(JNIEnv * env, jclass cls,
        jint width, jint height, jfloat side, jfloat upper, jfloat lower) {
    int r = -1;
    if (!g_distance && !g_lanes) {
        __android_log_print(ANDROID_LOG_DEBUG, "ADAS", "init");

        g_distance = (CarDistance *)malloc(sizeof(CarDistance) *maxResult);
        g_lanes   = (Lane *)malloc(sizeof(Lane) *maxResult);

        r = adas_init(width, height, side, upper, lower);   //car detection


        if (!init_lane_detection(width, height))
        {
            r   = -1;
        }
    }

    return r;
}

/*
 * Class:     ADASWrapper
 * Method:    ECDSASignToBuffer
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT void JNICALL Java_com_tencent_adas_ADASWrapper_uninit(JNIEnv * env, jclass cls) {
    adas_free();
    free(g_distance);
    g_distance = NULL;

    deinit_lane_detection();
    free(g_lanes);
    g_lanes = NULL;
}

/*
 * Class:     ADASWrapper
 * Method:    LaneDetectNative
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_tencent_adas_ADASWrapper_LaneDetectNative(JNIEnv * env, jclass cls,
        jobjectArray jResults, jlong elapsedRealtimeNanos, jbyteArray frame) {
    int returnCount = -1;
	if (frame == NULL )
		return returnCount;

    //jobjectArray jResults   = NULL;
    jsize bufLen    = (env)->GetArrayLength(frame);
    if (bufLen > 0)
    {
        jbyte* byteFrame =  (env)->GetByteArrayElements(frame, 0);
        if (byteFrame)
        {
            if (g_lanes)
            {
                memset(g_lanes, 0, sizeof(Lane) * maxResult );
                unsigned int resultCount = 0;
                unsigned int departure = 0;
                bool result = lane_detection_process((uint8_t *)byteFrame, g_lanes, &resultCount, &departure);

                if (true == result && 0 < resultCount) {
                    returnCount = resultCount < maxResult ? resultCount : maxResult;

                    jclass objectClass = (env)->FindClass("com/tencent/adas/Lane");
                    //jResults = (env)->NewObjectArray((jsize) returnCount, objectClass, NULL);

                    if (jResults)
                    {
                        for (int i=0; i<returnCount; ++i){
                            // constructor
                            jmethodID cid = (env)->GetMethodID(objectClass, "<init>", "(IIIII)V");
                            jobject jLane   = (env)->NewObject(objectClass, cid, g_lanes[i].x1, g_lanes[i].y1, g_lanes[i].x2, g_lanes[i].y2, departure);
                            __android_log_print(ANDROID_LOG_DEBUG, "ADAS", "lane", g_lanes[i].x1, g_lanes[i].y1, g_lanes[i].x2, g_lanes[i].y2, departure);
                            (env)->SetObjectArrayElement(jResults, i, jLane);
                            (env)->DeleteLocalRef(jLane);
                        }

                        //(env)->DeleteLocalRef(jResults);
                        //env->DeleteLocalRef(objectClass);
                    }
                }
            }
        }
    }

	return returnCount;
}

/*
 * Class:     ADASWrapper
 * Method:    carDetect
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_tencent_adas_ADASWrapper_carDetectNative(JNIEnv * env, jclass cls,
        jobjectArray jResults, jlong elapsedRealtimeNanos, jbyteArray frame) {
    int returnCount = -1;

	if (frame == NULL )
		return returnCount;

    //jobjectArray jResults   = NULL;
    jsize bufLen    = (env)->GetArrayLength(frame);
    if (bufLen > 0)
    {
        jbyte* byteFrame =  (env)->GetByteArrayElements(frame, 0);
        if (byteFrame)
        {

            if (g_distance)
            {
                memset(g_distance, 0, sizeof(CarDistance) *maxResult );
                unsigned int resultCount = 0;
                float   timetocrash = 0;

                struct timespec start;
                struct timespec end;
                float elapsedSeconds;

                if(clock_gettime(CLOCK_MONOTONIC, &start)){ /* handle error */ }
                //  TODO: 传入elapsedRealtimeNanos作为拍照时间
                int result  = adas_car_detect((uint8_t*)byteFrame, g_distance, &resultCount, &timetocrash);
                if(clock_gettime(CLOCK_MONOTONIC, &end)){ /* handle error */ }
                elapsedSeconds = (int64_t(end.tv_sec)*1000000000LL + end.tv_nsec - int64_t(start.tv_sec)*1000000000LL -start.tv_nsec)/1000000000.0;

                returnCount = 0;
                if (0 == result && 0 < resultCount) {
                    returnCount = resultCount < maxResult ? resultCount : maxResult;

                    jclass objectClass = (env)->FindClass("com/tencent/adas/CarDistance");
                    //jResults = (env)->NewObjectArray((jsize) returnCount, objectClass, NULL);
                    if (jResults)
                    {
                        for (int i = 0; i < returnCount; ++i){
                            jmethodID constructor = (env)->GetMethodID(objectClass, "<init>", "(IIIIIF)V");
                            //  TODO: tiemtocrash 应该换成每个CarDistance有一个数值，而不是现在只用最近的数值
                            __android_log_print(ANDROID_LOG_DEBUG, "ADAS", "adas_car_detect %d, %d, %d", g_distance[i].x, g_distance[i].y, g_distance[i].distance);
                            //if (g_distance[i].distance == 0) {
                            //    __android_log_print(ANDROID_LOG_DEBUG, "######_adas", "adas_car_detect0 %d, %d, %d", returnCount, resultCount, maxResult);
                            //}
                            jobject jDistance = (env)->NewObject(objectClass, constructor, g_distance[i].x, g_distance[i].y, g_distance[i].width, g_distance[i].height, g_distance[i].distance, timetocrash);
                            (env)->SetObjectArrayElement(jResults, i, jDistance);
                            (env)->DeleteLocalRef(jDistance);
                        }

                        //(env)->DeleteLocalRef(jResults);
                        //env->DeleteLocalRef(objectClass);
                    }
                }
            }
        }
    }

	return returnCount;
}



#ifdef __cplusplus
}
#endif
