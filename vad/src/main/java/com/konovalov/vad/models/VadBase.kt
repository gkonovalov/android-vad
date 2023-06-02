package com.konovalov.vad.models

import com.konovalov.vad.VadListener
import com.konovalov.vad.Validator.validateFrameSize
import com.konovalov.vad.Validator.validateSampleRate
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.SampleRate

internal abstract class VadBase(
    sampleRate: SampleRate,
    frameSize: FrameSize,
    mode: Mode,
    speechDurationMs: Int,
    silenceDurationMs: Int
) : VadModel {

    private var speechFramesCount: Long = 0
    private var silenceFramesCount: Long = 0
    private var maxSpeechFramesCount = 0
    private var maxSilenceFramesCount = 0

    override fun setContinuousSpeechListener(audio: ShortArray, listener: VadListener) {
        if (isSpeech(audio)) {
            silenceFramesCount = 0
            if (++speechFramesCount > maxSpeechFramesCount) {
                speechFramesCount = 0
                listener.onSpeechDetected()
            }
        } else {
            speechFramesCount = 0
            if (++silenceFramesCount > maxSilenceFramesCount) {
                silenceFramesCount = 0
                listener.onNoiseDetected()
            }
        }
    }

    override var sampleRate: SampleRate = sampleRate
        set(sampleRate) {
            validateSampleRate(model, sampleRate)
            field = sampleRate
        }

    override var frameSize: FrameSize = frameSize
        set(frameSize) {
            validateFrameSize(model, sampleRate, frameSize)
            field = frameSize
        }

    override var mode: Mode = mode
        set(mode) {
            field = mode
        }

    override var speechDurationMs: Int = speechDurationMs
        get() = getDurationMs(maxSpeechFramesCount)
        set(speechDurationMs) {
            check(speechDurationMs >= 0) {
                "Parameter speechDurationMs can't be below zero!"
            }

            field = speechDurationMs
            maxSpeechFramesCount = getFramesCount(speechDurationMs)
        }

    override var silenceDurationMs: Int = silenceDurationMs
        get() = getDurationMs(maxSilenceFramesCount)
        set(silenceDurationMs) {
            check(silenceDurationMs >= 0) {
                "Parameter silenceDurationMs can't be below zero!"
            }

            field = silenceDurationMs
            maxSilenceFramesCount = getFramesCount(silenceDurationMs)
        }

    private fun getDurationMs(frameCount: Int): Int {
        return frameCount * (frameSize.value / (sampleRate.value / 1000))
    }

    private fun getFramesCount(durationMs: Int): Int {
        return durationMs / (frameSize.value / (sampleRate.value / 1000))
    }

    init {
        this.sampleRate = sampleRate
        this.frameSize = frameSize
        this.mode = mode
        this.silenceDurationMs = silenceDurationMs
        this.speechDurationMs = speechDurationMs
    }
}