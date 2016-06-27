## for build, run in shell:
##
##  cd ./adas/src/
##  GNUMAKE -f ${NDK}/build/core/build-local.mk
##  mv libs/x86/libTXADASService.so $(LOCAL_PATH)/../main/jniLibs/x86
##  cp ./jni/openssl/lib/android/*.* ../libs/armeabi/
##
##

LOCAL_PATH := $(call my-dir)
#$(warning ######################## build path $(LOCAL_PATH) ...)

#include $(CLEAR_VARS)
#LOCAL_MODULE := kneron_adas
#LOCAL_SRC_FILES := $(LOCAL_PATH)/libs/libkneron_adas.so
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := kneron_adas_car
LOCAL_SRC_FILES := $(LOCAL_PATH)/libs/armeabi-v7a/libkneron_adas_car.so
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := kneron_adas_lane
LOCAL_SRC_FILES := $(LOCAL_PATH)/libs/armeabi-v7a/libkneron_adas_lane.so
#include $(PREBUILT_SHARED_LIBRARY)

#### module TXADASService
include $(CLEAR_VARS)

#LOCAL_C_INCLUDES := $(LOCAL_PATH)/Tencent_iot_SDK/include/

LOCAL_LDLIBS := -L$(LOCAL_PATH)/libs/armeabi -lkneron_adas_car -lkneron_adas_lane
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog

LOCAL_MODULE    := TXADASService
LOCAL_SRC_FILES := \
    jni_tencentADAS.cpp

#$(warning ######################## build module $(LOCAL_MODULE) ...)

include $(BUILD_SHARED_LIBRARY)