package com.konovalov.vad.models

import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate

internal class VadWebRTC(
    sampleRate: SampleRate,
    frameSize: FrameSize,
    mode: Mode,
    speechDurationMs: Int,
    silenceDurationMs: Int
) : VadModel(
    sampleRate,
    frameSize,
    mode,
    speechDurationMs,
    silenceDurationMs
) {

    private var nativeHandle: Long = -1

    init {
        nativeHandle = nativeInit()
        require(isVadInitialized()) { "Error can't init WebRTC VAD!!" }
        setMode()
    }

    override fun isSpeech(audioData: ShortArray): Boolean {
        if (isVadInitialized()) {
            return nativeIsSpeech(nativeHandle, sampleRate.value, frameSize.value, audioData)
        }
        return false
    }

    override var mode: Mode
        get() = super.mode
        set(mode) {
            super.mode = mode
            setMode()
        }

    override val model: Model
        get() = Model.WEB_RTC_GMM

    override fun close() {
        if (isVadInitialized()) {
            nativeDestroy(nativeHandle)
            nativeHandle = -1
        }
    }

    private fun setMode() {
        if (isVadInitialized()) {
            nativeSetMode(nativeHandle, mode.value)
        }
    }

    private fun isVadInitialized(): Boolean = nativeHandle > 0

    private external fun nativeInit(): Long
    private external fun nativeSetMode(nativeHandle: Long, mode: Int): Boolean
    private external fun nativeIsSpeech(nativeHandle: Long, sampleRate: Int, frameSize: Int, audio: ShortArray?): Boolean
    private external fun nativeDestroy(nativeHandle: Long)

    companion object {
        init {
            System.loadLibrary("vad_jni")
        }
    }
}