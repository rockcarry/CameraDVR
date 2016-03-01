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
    $(LOCAL_PATH)/ffjpegdec

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libui \
    libgui \
    libandroid_runtime

ifeq ($(CONFIG_HW_PLAT),a31)
LOCAL_C_INCLUDES += \
    frameworks/av/media/CedarX-Projects/CedarX/include \
    frameworks/av/media/CedarX-Projects/CedarX/include/include_system

LOCAL_SRC_FILES += \
    ffjpegdec/$(CONFIG_HW_PLAT)/ffjpegdec.c \
    ffjpegdec/$(CONFIG_HW_PLAT)/LibveDecoder.c \
    ffjpegdec/$(CONFIG_HW_PLAT)/formatconvert.c

LOCAL_SHARED_LIBRARIES += \
    libsunxi_alloc \
    libcedarxbase \
    libcedarxosal \
    libcedarv
endif

ifeq ($(CONFIG_HW_PLAT),libjpeg)
LOCAL_C_INCLUDES += \
    external/jpeg

LOCAL_SRC_FILES += \
    ffjpegdec/$(CONFIG_HW_PLAT)/ffjpegdec.c

LOCAL_SHARED_LIBRARIES += \
    libjpeg
endif

include $(BUILD_SHARED_LIBRARY)

