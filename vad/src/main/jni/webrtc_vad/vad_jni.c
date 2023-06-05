#include "webrtc_vad/common_audio/vad/include/webrtc_vad.h"
#include "webrtc_vad/common_audio/signal_processing/include/signal_processing_library.h"
#include <stdlib.h>
#include <jni.h>

JNIEXPORT jlong JNICALL
Java_com_konovalov_vad_models_VadWebRTC_nativeInit(JNIEnv *env, jobject object) {
    VadInst *vad = WebRtcVad_Create();

    if (vad == NULL) {
        return -1;
    }

    int status = WebRtcVad_Init(vad);
    if (status != 0) {
        WebRtcVad_Free(vad);
        return -1;
    }

    return (jlong) vad;
}

JNIEXPORT void JNICALL
Java_com_konovalov_vad_models_VadWebRTC_nativeDestroy(JNIEnv *env, jobject object, jlong vad) {
    WebRtcVad_Free((VadInst *) vad);
}

JNIEXPORT jboolean JNICALL
Java_com_konovalov_vad_models_VadWebRTC_nativeSetMode(JNIEnv *env, jobject object, jlong vad, jint jMode) {
    return WebRtcVad_set_mode((VadInst *) vad, jMode) >= 0;
}

JNIEXPORT jboolean JNICALL
Java_com_konovalov_vad_models_VadWebRTC_nativeIsSpeech(JNIEnv *env, jobject object, jlong vad, jint jSampleRate, jint jFrameSize, jshortArray bytes) {
    int sampleRate = jSampleRate;
    int frameSize = jFrameSize;

    jshort *audioFrame = (*env)->GetShortArrayElements(env, bytes, 0);

    int resultVad = WebRtcVad_Process((VadInst *) vad,
                                      sampleRate,
                                      audioFrame,
                                      (size_t) frameSize);

    (*env)->ReleaseShortArrayElements(env, bytes, audioFrame, 0);

    if (resultVad > 0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}