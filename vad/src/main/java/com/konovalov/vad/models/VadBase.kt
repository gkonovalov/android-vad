package com.konovalov.vad.models

import com.konovalov.vad.VadListener
import com.konovalov.vad.Validator.validateFrameSize
import com.konovalov.vad.Validator.validateSampleRate
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.SampleRate

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * Abstract base class for VAD models.
 * </p>
 * @param sampleRate The sample rate of the audio.
 * @param frameSize The frame size of the audio.
 * @param mode The detection mode of the VAD.
 * @param speechDurationMs The duration in milliseconds for speech to be considered continuous.
 * @param silenceDurationMs The duration in milliseconds for silence to be considered continuous.
 */
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

    /**
     * <p>
     * Sets a continuous speech listener to monitor the audio.
     * </p>
     * @param audio The audio data as a ShortArray.
     * @param listener The listener to be notified when speech or noise is detected.
     */
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


    /**
     * <p>
     * Set, retrieve and validate sample rate for Vad Model.
     * </p>
     * @param sampleRate The sample rate as a SampleRate.
     * @throws IllegalArgumentException if there was invalid sample rate.
     */
    override var sampleRate: SampleRate = sampleRate
        set(sampleRate) {
            validateSampleRate(model, sampleRate)
            field = sampleRate
        }

    /**
     * <p>
     * Set, retrieve and validate frame size for Vad Model.
     * </p>
     * @param frameSize The sample rate as a FrameSize.
     * @throws IllegalArgumentException if there was invalid frame size.
     */
    override var frameSize: FrameSize = frameSize
        set(frameSize) {
            validateFrameSize(model, sampleRate, frameSize)
            field = frameSize
        }

    /**
     * <p>
     * Set and retrieve mode for Vad Model.
     * </p>
     * @param mode The sample rate as a Mode.
     */
    override var mode: Mode = mode
        set(mode) {
            field = mode
        }

    /**
     * <p>
     * Set, retrieve and validate speechDurationMs for Vad Model.
     * The value of this parameter will define the necessary and sufficient duration of positive
     * results to recognize result as speech. Negative numbers are not allowed.
     * </p>
     * @param speechDurationMs The speech duration ms as a Int.
     * @throws IllegalArgumentException if there was negative numbers.
     */
    override var speechDurationMs: Int = speechDurationMs
        get() = getDurationMs(maxSpeechFramesCount)
        set(speechDurationMs) {
            check(speechDurationMs >= 0) {
                "Parameter speechDurationMs can't be below zero!"
            }

            field = speechDurationMs
            maxSpeechFramesCount = getFramesCount(speechDurationMs)
        }

    /**
     * <p>
     * Set, retrieve and validate silenceDurationMs for Vad Model.
     * The value of this parameter will define the necessary and sufficient duration of
     * negative results to recognize it as silence. Negative numbers are not allowed.
     * </p>
     * @param silenceDurationMs The silence duration ms as a Int.
     * @throws IllegalArgumentException if there was negative numbers.
     */
    override var silenceDurationMs: Int = silenceDurationMs
        get() = getDurationMs(maxSilenceFramesCount)
        set(silenceDurationMs) {
            check(silenceDurationMs >= 0) {
                "Parameter silenceDurationMs can't be below zero!"
            }

            field = silenceDurationMs
            maxSilenceFramesCount = getFramesCount(silenceDurationMs)
        }

    /**
     * <p>
     * Calculates the duration in milliseconds based on the frame count, frequency and frame size.
     * </p>
     * @param frameCount The number of frames.
     * @return The duration in milliseconds.
     */
    private fun getDurationMs(frameCount: Int): Int {
        return frameCount * (frameSize.value / (sampleRate.value / 1000))
    }

    /**
     * <p>
     * Calculates the frame count based on the duration in milliseconds, frequency and frame size.
     * </p>
     * @param durationMs The duration in milliseconds.
     * @return The frame count.
     */
    private fun getFramesCount(durationMs: Int): Int {
        return durationMs / (frameSize.value / (sampleRate.value / 1000))
    }

    /**
     * <p>
     * Initializes the VAD model settings with the provided parameters.
     * </p>
     */
    init {
        this.sampleRate = sampleRate
        this.frameSize = frameSize
        this.mode = mode
        this.silenceDurationMs = silenceDurationMs
        this.speechDurationMs = speechDurationMs
    }
}