#include <android_runtime/AndroidRuntime.h>
#include <android_runtime/android_graphics_SurfaceTexture.h>
#include <android_runtime/android_view_Surface.h>

#include "com_apical_cdr_CamCdr.h"
#include "camcdr.h"

#define DO_USE_VAR(v) do { v = v; } while (0)

/*
 * Class:     com_apical_cdr_CamCdr
 * Method:    nativeInit
 * Signature: (Ljava/lang/String;IIII)J
 */
JNIEXPORT jlong JNICALL Java_com_apical_cdr_CamCdr_nativeInit
  (JNIEnv *env, jclass cls, jstring dev, jint sub, jint fmt, jint w, jint h) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    return (jlong) camcdr_init(env->GetStringUTFChars(dev, NULL), sub, fmt, w, h);
}

/*
 * Class:     com_apical_cdr_CamCdr
 * Method:    nativeClose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_CamCdr_nativeClose
  (JNIEnv *env, jclass cls, jlong dev) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    camcdr_close((CAMCDR*)dev);
}

/*
 * Class:     com_apical_cdr_CamCdr
 * Method:    nativeSetPreviewSurface
 * Signature: (JLjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_CamCdr_nativeSetPreviewSurface
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

    camcdr_set_preview_target((CAMCDR*)dev, gbp);
}

/*
 * Class:     com_apical_cdr_CamCdr
 * Method:    nativeSetPreviewTexture
 * Signature: (JLjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_CamCdr_nativeSetPreviewTexture
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

    camcdr_set_preview_target((CAMCDR*)dev, gbp);
}

/*
 * Class:     com_apical_cdr_CamCdr
 * Method:    nativeStartPreview
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_CamCdr_nativeStartPreview
  (JNIEnv *env, jclass cls, jlong dev) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    camcdr_start_preview((CAMCDR*)dev);
}

/*
 * Class:     com_apical_cdr_CamCdr
 * Method:    nativeStopPreview
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_apical_cdr_CamCdr_nativeStopPreview
  (JNIEnv *env, jclass cls, jlong dev) {
    DO_USE_VAR(env);
    DO_USE_VAR(cls);
    camcdr_start_preview((CAMCDR*)dev);
}