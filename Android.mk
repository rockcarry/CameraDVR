LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := CameraCDR

LOCAL_CERTIFICATE := platform

LOCAL_JNI_SHARED_LIBRARIES := libusbcam_jni
LOCAL_REQUIRED_MODULES := libusbcam_jni

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
