package com.konovalov.vad

import android.content.Context
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate
import com.konovalov.vad.models.VadModel
import com.konovalov.vad.models.VadSilero
import com.konovalov.vad.models.VadWebRTC

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 *  A builder class for creating VadModel instances.
 *  This class allows setting various parameters for the VAD, such as
 *  the model, sample rate, frame size, mode, speech duration, and silence duration.
 *  Use the builder function to create an instance of this builder, and then chain the
 *  setter methods to set the desired parameters. Finally, call the build method to construct
 *  the appropriate VAD model based on the provided parameters.
 *
 * The Silero VAD supports the following parameters:
 * Sample Rates: 8000Hz, 16000Hz
 * Frame Sizes (per sample rate):
 *             For 8000Hz: 256, 512, 768
 *             For 16000Hz: 512, 1024, 1536
 * Mode: NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * The WebRTC VAD supports the following parameters:
 * Sample Rates: 8000Hz, 16000Hz, 32000Hz, 48000Hz
 * Frame Sizes (per sample rate):
 *             For 8000Hz: 80, 160, 240
 *             For 16000Hz: 160, 320, 480
 *             For 32000Hz: 320, 640, 960
 *             For 48000Hz: 480, 960, 1440
 * Mode: NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 * </p>
 * @param model (required) - select proper Vad model for building.
 * @param sampleRate (required) - The sample rate of the audio input.
 * @param frameSize (required) - The frame size of the audio input.
 * @param mode (required) - The mode of the VAD model.
 * @param context (optional) - The context is required for VadSilero
 * @param speechDurationMs (optional) - used in Continuous Speech detector, the value of this
 * parameter will define the necessary and sufficient duration of negative results
 * to recognize it as silence. Negative numbers are not allowed.
 * @param silenceDurationMs (optional) - used in Continuous Speech detector, the value of
 * this parameter will define the necessary and sufficient duration of positive results to
 * recognize result as speech. Negative numbers are not allowed.
 * </p>
 */
class Vad private constructor() {
    private var context: Context? = null
    private lateinit var model: Model
    private lateinit var sampleRate: SampleRate
    private lateinit var frameSize: FrameSize
    private lateinit var mode: Mode
    private var speechDurationMs = 0
    private var silenceDurationMs = 0

    fun setModel(model: Model): Vad = apply {
        this.model = model
    }

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
     * @throws IllegalArgumentException if the context is null and the model is Model.SILERO_DNN.
     */
    fun build(): VadModel {
        require(!(context == null && model == Model.SILERO_DNN)) {
            "Context is required for Model.SILERO_DNN!"
        }

        return when (model) {
            Model.WEB_RTC_GMM -> VadWebRTC(
                sampleRate,
                frameSize,
                mode,
                speechDurationMs,
                silenceDurationMs
            )

            Model.SILERO_DNN -> VadSilero(
                context!!,
                sampleRate,
                frameSize,
                mode,
                speechDurationMs,
                silenceDurationMs
            )
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Vad {
            return Vad()
        }
    }
}