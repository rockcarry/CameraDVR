#include <android_runtime/AndroidRuntime.h>
#include <android_runtime/android_view_Surface.h>

#include "com_apical_cdr_UsbCam.h"
#include "usbcam.h"

#define DO_USE_VAR(v) do { v = v; } while (0)

/*
 * Class:     com_apical_cdr_UsbCam
 * Method:    nativeInit
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jlong JNICALL Java_com_apical_cdr_UsbCam_nativeInit
  (JNIEnv *env, jclass cls, jstring dev) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    return (jlong) usbcam_init(env->GetStringUTFChars(dev, NULL));
}

/*
 * Class:     com_apical_cdr_UsbCam
 * Method:    nativeClose
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_UsbCam_nativeClose
  (JNIEnv *env, jclass cls, jlong dev) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    usbcam_close((USBCAM*)dev);
}

/*
 * Class:     com_apical_cdr_UsbCam
 * Method:    nativeSetPreviewSurface
 * Signature: (ILjava/lang/Object;II)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_UsbCam_nativeSetPreviewSurface
  (JNIEnv *env, jclass cls, jlong dev, jobject surface, jint w, jint h) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    sp<Surface> surf = surface ? android_view_Surface_getSurface(env, surface) : NULL;
    if (android::Surface::isValid(surf)) {
		ALOGE("surface is valid .");
	} else {
		ALOGE("surface is invalid.");
	}
    usbcam_set_preview_display((USBCAM*)dev, surf, w, h);
}

/*
 * Class:     com_apical_cdr_UsbCam
 * Method:    nativeStartPreview
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_UsbCam_nativeStartPreview
  (JNIEnv *env, jclass cls, jlong dev) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    usbcam_start_preview((USBCAM*)dev);
}

/*
 * Class:     com_apical_cdr_UsbCam
 * Method:    nativeStopPreview
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_UsbCam_nativeStopPreview
  (JNIEnv *env, jclass cls, jlong dev) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    usbcam_start_preview((USBCAM*)dev);
}