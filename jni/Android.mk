LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libusbcam_jni

LOCAL_SRC_FILES := \
    usbcam.cpp \
    com_apical_cdr_UsbCam.cpp

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libui \
    libgui \
    libandroid_runtime

include $(BUILD_SHARED_LIBRARY)

