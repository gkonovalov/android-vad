package com.konovalov.vad.silero

import android.content.Context
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate

/**
 * Created by Georgiy Konovalov on 26/06/2023.
 * <p>
 * YAMNet is a deep net that predicts 521 audio event classes(such as Speech, Music, Clapping, etc)
 * from the AudioSet-YouTube corpus it was trained on. It employs the Mobilenet_v1
 * depthwise-separable convolution architecture.
 *
 * The YAMNet VAD supports the following parameters:
 *
 * Sample Rates: 16000Hz
 *
 * Frame Sizes (per sample rate):
 *             For 16000Hz: 243, 487, 731, 975
 *
 * Mode: OFF, NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 * </p>
 * @param context (required) - The context is required for VadYamnet
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
    private lateinit var context: Context
    private lateinit var sampleRate: SampleRate
    private lateinit var frameSize: FrameSize
    private lateinit var mode: Mode
    private var speechDurationMs = 0
    private var silenceDurationMs = 0

    fun setContext(context: Context): Vad = apply {
        this.context = context.applicationContext ?: context
    }

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
    fun build(): VadSilero {
        return VadSilero(
            context,
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