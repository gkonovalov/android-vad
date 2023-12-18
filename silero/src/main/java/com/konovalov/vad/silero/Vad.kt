package com.konovalov.vad.silero

import android.content.Context
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate

/**
 * Created by Georgiy Konovalov on 26/06/2023.
 * <p>
 * The Silero VAD algorithm, based on DNN, analyzes the audio signal to determine whether it
 * contains speech or non-speech segments. It offers higher accuracy in differentiating speech from
 * background noise compared to the WebRTC VAD algorithm.
 *
 * The Silero VAD supports the following parameters:
 *
 * Sample Rates: 8000Hz, 16000Hz
 *
 * Frame Sizes (per sample rate):
 *             For 8000Hz: 256, 512, 768
 *             For 16000Hz: 512, 1024, 1536
 * Mode: OFF, NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 * </p>
 * @param context (required) The context helps with reading the model file from the file system.
 * @param sampleRate (required) The sample rate of the audio input.
 * @param frameSize (required) The frame size of the audio input.
 * @param mode (required) The recognition mode of the VAD model.
 * @param speechDurationMs (optional) The minimum duration in milliseconds for speech segments.
 * @param silenceDurationMs (optional) The minimum duration in milliseconds for silence segments.
 * </p>
 */
class Vad private constructor() {
    private lateinit var context: Context
    private lateinit var sampleRate: SampleRate
    private lateinit var frameSize: FrameSize
    private lateinit var mode: Mode
    private var speechDurationMs = 0
    private var silenceDurationMs = 0

    /**
     * <p>
     * Set Context for Vad Model.
     * </p>
     * @param context (required) - The context is required and helps with reading the
     *                             model file from the file system.
     */
    fun setContext(context: Context): Vad = apply {
        this.context = context.applicationContext ?: context
    }

    /**
     * <p>
     * Set sample rate of the audio input for Vad Model.
     *
     * Sample Rates: 8000Hz, 16000Hz
     * </p>
     * @param sampleRate (required) - The sample rate of the audio input.
     */
    fun setSampleRate(sampleRate: SampleRate): Vad = apply {
        this.sampleRate = sampleRate
    }

    /**
     * <p>
     * Set frame size of the audio input for Vad Model.
     *
     * Valid Frame Sizes (per sample rate):
     *             For 8000Hz: 256, 512, 768
     *             For 16000Hz: 512, 1024, 1536
     * </p>
     * @param frameSize (required) - The frame size of the audio input.
     */
    fun setFrameSize(frameSize: FrameSize): Vad = apply {
        this.frameSize = frameSize
    }

    /**
     * <p>
     * Set recognition mode for Vad Model.
     *
     * Valid Mode: OFF, NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
     * </p>
     * @param mode (required) - The recognition mode of the VAD model.
     */
    fun setMode(mode: Mode): Vad = apply {
        this.mode = mode
    }

    /**
     * <p>
     * Set the minimum duration in milliseconds for speech segments.
     * The value of this parameter will define the necessary and sufficient duration of positive
     * results to recognize result as speech. Negative numbers are not allowed.
     *
     * Parameters used in {@link VadSilero.continuousSpeechListener}.
     * </p>
     * @param speechDurationMs (optional) The minimum duration in milliseconds for speech segments.
     */
    fun setSpeechDurationMs(speechDurationMs: Int): Vad = apply {
        this.speechDurationMs = speechDurationMs
    }

    /**
     * <p>
     * Set the minimum duration in milliseconds for silence segments.
     * The value of this parameter will define the necessary and sufficient duration of
     * negative results to recognize it as silence. Negative numbers are not allowed.
     *
     * Parameters used in {@link VadSilero.continuousSpeechListener}.
     * </p>
     * @param silenceDurationMs (optional) The minimum duration in milliseconds for silence segments.
     */
    fun setSilenceDurationMs(silenceDurationMs: Int): Vad = apply {
        this.silenceDurationMs = silenceDurationMs
    }

    /**
     * <p>
     * Builds and returns a VadModel instance based on the specified parameters.
     * </p>
     * @return An {@link VadSilero} with constructed VadModel.
     * @throws IllegalArgumentException If invalid parameters have been set for the model.
     * @throws OrtException If the model failed to parse, wasn't compatible or caused an error.
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