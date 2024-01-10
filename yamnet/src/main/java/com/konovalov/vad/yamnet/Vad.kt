package com.konovalov.vad.yamnet

import android.content.Context
import com.konovalov.vad.yamnet.config.FrameSize
import com.konovalov.vad.yamnet.config.Mode
import com.konovalov.vad.yamnet.config.SampleRate

/**
 * Created by Georgiy Konovalov on 6/26/2023.
 *
 * YAMNet is a deep net that predicts 521 audio event classes(such as Speech, Music, Clapping, etc)
 * from the AudioSet-YouTube corpus it was trained on. It employs the Mobilenet_v1
 * depthwise-separable convolution architecture.
 *
 * The YAMNet VAD supports the following parameters:
 *
 * Sample Rates:
 *
 *      16000Hz
 *
 * Frame Sizes (per sample rate):
 *
 *      For 16000Hz: 243, 487, 731, 975
 *
 * Mode:
 *
 *    NORMAL,
 *    LOW_BITRATE,
 *    AGGRESSIVE,
 *    VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 *
 * @param context           is required for reading the model file from file system.
 * @param sampleRate        is required for processing audio input.
 * @param frameSize         is required for processing audio input.
 * @param mode              is required for the VAD model.
 * @param speechDurationMs  is minimum duration in milliseconds for speech segments (optional).
 * @param silenceDurationMs is minimum duration in milliseconds for silence segments (optional).
 */
class Vad private constructor() {
    private lateinit var context: Context
    private lateinit var sampleRate: SampleRate
    private lateinit var frameSize: FrameSize
    private lateinit var mode: Mode
    private var speechDurationMs = 0
    private var silenceDurationMs = 0

    /**
     * Set Context for Vad Model.
     *
     * @param context is required for accessing the model file.
     */
    fun setContext(context: Context): Vad = apply {
        this.context = context.applicationContext ?: context
    }

    /**
     * Set, retrieve and validate sample rate for Vad Model.
     *
     * Valid Sample Rates:
     *
     *      16000Hz
     *
     * @param sampleRate is required for processing audio input.
     */
    fun setSampleRate(sampleRate: SampleRate): Vad = apply {
        this.sampleRate = sampleRate
    }

    /**
     * Set, retrieve and validate frame size for Vad Model.
     *
     * Valid Frame Sizes (per sample rate):
     *
     *             For 16000Hz: 243, 487, 731, 975
     *
     * @param frameSize is required for processing audio input.
     */
    fun setFrameSize(frameSize: FrameSize): Vad = apply {
        this.frameSize = frameSize
    }

    /**
     * Set and retrieve detection mode for Vad model.
     *
     * Mode:
     *
     *    NORMAL,
     *    LOW_BITRATE,
     *    AGGRESSIVE,
     *    VERY_AGGRESSIVE
     *
     * @param mode is required for the VAD model.
     */
    fun setMode(mode: Mode): Vad = apply {
        this.mode = mode
    }

    /**
     * Set the minimum duration in milliseconds for speech segments.
     * The value of this parameter will define the necessary and sufficient duration of positive
     * results to recognize result as speech. This parameter is optional.
     *
     * Permitted range (0ms >= speechDurationMs <= 300000ms).
     *
     * Parameters used for {@link VadSilero.isSpeech}.
     *
     * @param speechDurationMs minimum duration in milliseconds for speech segments.
     */
    fun setSpeechDurationMs(speechDurationMs: Int): Vad = apply {
        this.speechDurationMs = speechDurationMs
    }

    /**
     * Set the minimum duration in milliseconds for silence segments.
     * The value of this parameter will define the necessary and sufficient duration of
     * negative results to recognize it as silence. This parameter is optional.
     *
     * Permitted range (0ms >= silenceDurationMs <= 300000ms).
     *
     * Parameters used in {@link VadSilero.isSpeech}.
     *
     * @param silenceDurationMs minimum duration in milliseconds for silence segments.
     */
    fun setSilenceDurationMs(silenceDurationMs: Int): Vad = apply {
        this.silenceDurationMs = silenceDurationMs
    }

    /**
     * Builds and returns a VadModel instance based on the specified parameters.
     *
     * @return constructed VadYamnet model.
     * @throws IllegalArgumentException if there was an error during initialization of VAD.
     */
    fun build(): VadYamnet {
        return VadYamnet(
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