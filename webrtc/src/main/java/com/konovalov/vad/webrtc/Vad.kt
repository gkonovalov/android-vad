package com.konovalov.vad.webrtc

import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * The WebRTC VAD algorithm, based on GMM, analyzes the audio signal to determine whether it
 * contains speech or non-speech segments.
 *
 * The WebRTC VAD supports the following parameters:
 *
 * Sample Rates: 8000Hz, 16000Hz, 32000Hz, 48000Hz
 *
 * Frame Sizes (per sample rate):
 *             For 8000Hz: 80, 160, 240
 *             For 16000Hz: 160, 320, 480
 *             For 32000Hz: 320, 640, 960
 *             For 48000Hz: 480, 960, 1440
 * Mode: NORMAL, LOW_BITRATE, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 * </p>
 * @param sampleRate (required) - The sample rate of the audio input.
 * @param frameSize (required) - The frame size of the audio input.
 * @param mode (required) - The mode of the VAD model.
 * @param speechDurationMs (optional) - used in Continuous Speech detector, the value of this
 * parameter will define the necessary and sufficient duration of negative results
 * to recognize it as silence. Negative numbers are not allowed.
 * @param silenceDurationMs (optional) - used in Continuous Speech detector, the value of
 * this parameter will define the necessary and sufficient duration of positive results to
 * recognize result as speech. Negative numbers are not allowed.
 * </p>
 */
class Vad private constructor() {
    private lateinit var sampleRate: SampleRate
    private lateinit var frameSize: FrameSize
    private lateinit var mode: Mode
    private var speechDurationMs = 0
    private var silenceDurationMs = 0

    fun setSampleRate(sampleRate: SampleRate): Vad = apply {
        this.sampleRate = sampleRate
    }

    fun setFrameSize(frameSize: FrameSize): Vad = apply {
        this.frameSize = frameSize
    }

    fun setMode(mode: Mode): Vad = apply {
        this.mode = mode
    }

    fun setSpeechDurationMs(speechDurationMs: Int): Vad = apply {
        this.speechDurationMs = speechDurationMs
    }

    fun setSilenceDurationMs(silenceDurationMs: Int): Vad = apply {
        this.silenceDurationMs = silenceDurationMs
    }

    /**
     * <p>
     * Builds and returns a VadModel instance based on the specified parameters.
     * </p>
     * @return The constructed VadModel.
     */
    fun build(): VadWebRTC {
        return VadWebRTC(
            sampleRate,
            frameSize,
            mode,
            speechDurationMs,
            silenceDurationMs
        )
    }

    companion object {
        @JvmStatic
        fun builder(): Vad {
            return Vad()
        }
    }
}