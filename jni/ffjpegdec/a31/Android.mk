LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../../Config.mk

ifeq ($(CONFIG_HW_PLAT),a31)

include $(CLEAR_VARS)

LOCAL_MODULE := libffjpegdec

LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += \
    frameworks/av/media/CedarX-Projects/CedarX/include \
    frameworks/av/media/CedarX-Projects/CedarX/include/include_system
    
LOCAL_SRC_FILES := \
    ffjpegdec.c \
    formatconvert.c \
    LibveDecoder.c

include $(BUILD_STATIC_LIBRARY)

endif

