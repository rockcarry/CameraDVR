LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/Config.mk

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libcamcdr_jni

LOCAL_SRC_FILES := \
    camcdr.cpp \
    com_apical_cdr_CamCdr.cpp

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \

LOCAL_REQUIRED_MODULES := \
    libffjpegdec

LOCAL_STATIC_LIBRARIES := \
    libffjpegdec

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libui \
    libgui \
    libandroid_runtime

ifeq ($(CONFIG_HW_PLAT),a31)
LOCAL_SHARED_LIBRARIES += \
    libsunxi_alloc \
    libcedarxbase \
    libcedarxosal \
    libcedarv
endif

include $(BUILD_SHARED_LIBRARY)

