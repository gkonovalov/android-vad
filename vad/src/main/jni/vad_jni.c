#include "webrtc/common_audio/vad/include/webrtc_vad.h"
#include "webrtc/common_audio/signal_processing/include/signal_processing_library.h"
#include <stdlib.h>
#include <jni.h>

VadInst *internalHandle;

JNIEXPORT jboolean JNICALL
Java_com_konovalov_vad_models_VadWebRTC_nativeInit(JNIEnv *env, jobject object) {
    if (internalHandle) {
        WebRtcVad_Free(internalHandle);
    }

    internalHandle = WebRtcVad_Create();
    return WebRtcVad_Init(internalHandle) >= 0;
}

JNIEXPORT void JNICALL
Java_com_konovalov_vad_models_VadWebRTC_nativeDestroy(JNIEnv *env, jobject object) {
    if (internalHandle) {
        WebRtcVad_Free(internalHandle);
        internalHandle = NULL;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_konovalov_vad_models_VadWebRTC_nativeSetMode(JNIEnv *env, jobject object, jint jMode) {
    if (internalHandle != NULL) {
        return WebRtcVad_set_mode(internalHandle, jMode) >= 0;
    }

    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_konovalov_vad_models_VadWebRTC_nativeIsSpeech(JNIEnv *env,
                                                       jobject object,
                                                       jint jSampleRate,
                                                       jint jFrameSize,
                                                       jshortArray bytes) {
    int sampleRate = jSampleRate;
    int frameSize = jFrameSize;
    jshort *audioFrame = (*env)->GetShortArrayElements(env, bytes, 0);

    int resultVad = WebRtcVad_Process(internalHandle,
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