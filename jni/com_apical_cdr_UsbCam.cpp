#include <android_runtime/AndroidRuntime.h>
#include <android_runtime/android_graphics_SurfaceTexture.h>
#include <android_runtime/android_view_Surface.h>

#include "com_apical_cdr_UsbCam.h"
#include "usbcam.h"

#define DO_USE_VAR(v) do { v = v; } while (0)

/*
 * Class:     com_apical_cdr_UsbCam
 * Method:    nativeInit
 * Signature: (Ljava/lang/String;)J
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
 * Signature: (J)V
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
 * Signature: (JLjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_UsbCam_nativeSetPreviewSurface
  (JNIEnv *env, jclass cls, jlong dev, jobject jsurface) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);

    sp<IGraphicBufferProducer> gbp;
    sp<Surface> surface;
    if (jsurface) {
        surface = android_view_Surface_getSurface(env, jsurface);
        if (surface != NULL) {
            gbp = surface->getIGraphicBufferProducer();
        }
    }

    usbcam_set_preview_target((USBCAM*)dev, gbp);
}

/*
 * Class:     com_apical_cdr_UsbCam
 * Method:    nativeSetPreviewTexture
 * Signature: (JLjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_UsbCam_nativeSetPreviewTexture
  (JNIEnv *env, jclass cls, jlong dev, jobject jtexture) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);

    sp<IGraphicBufferProducer> gbp = NULL;
    if (jtexture != NULL) {
        gbp = SurfaceTexture_getProducer(env, jtexture);
        if (gbp == NULL) {
            ALOGW("SurfaceTexture already released in setPreviewTexture !");
            return;
        }
    }

    usbcam_set_preview_target((USBCAM*)dev, gbp);
}

/*
 * Class:     com_apical_cdr_UsbCam
 * Method:    nativeStartPreview
 * Signature: (J)V
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
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_UsbCam_nativeStopPreview
  (JNIEnv *env, jclass cls, jlong dev) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    usbcam_start_preview((USBCAM*)dev);
}