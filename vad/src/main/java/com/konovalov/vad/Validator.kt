package com.konovalov.vad

import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * Validator responsible for validating sample rates and frame sizes for different VAD models.
 * </p>
 */
object Validator {

    /**
     * <p>
     * Valid sample rates and frame sizes for WebRTC VAD GMM model.
     * </p>
     */
    private val WEB_RTC_VALID_FRAMES: Map<SampleRate, List<FrameSize>> = mapOf(
        SampleRate.SAMPLE_RATE_8K to listOf(
            FrameSize.FRAME_SIZE_80,
            FrameSize.FRAME_SIZE_160,
            FrameSize.FRAME_SIZE_240
        ),
        SampleRate.SAMPLE_RATE_16K to listOf(
            FrameSize.FRAME_SIZE_160,
            FrameSize.FRAME_SIZE_320,
            FrameSize.FRAME_SIZE_480
        ),
        SampleRate.SAMPLE_RATE_32K to listOf(
            FrameSize.FRAME_SIZE_320,
            FrameSize.FRAME_SIZE_640,
            FrameSize.FRAME_SIZE_960
        ),
        SampleRate.SAMPLE_RATE_48K to listOf(
            FrameSize.FRAME_SIZE_480,
            FrameSize.FRAME_SIZE_960,
            FrameSize.FRAME_SIZE_1440
        )
    )

    /**
     * <p>
     * Valid Sample Rates and Frame Sizes for Silero VAD DNN model.
     * </p>
     */
    private val SILERO_VALID_FRAMES: Map<SampleRate, List<FrameSize>> = mapOf(
        SampleRate.SAMPLE_RATE_8K to listOf(
            FrameSize.FRAME_SIZE_256,
            FrameSize.FRAME_SIZE_512,
            FrameSize.FRAME_SIZE_768
        ),
        SampleRate.SAMPLE_RATE_16K to listOf(
            FrameSize.FRAME_SIZE_512,
            FrameSize.FRAME_SIZE_1024,
            FrameSize.FRAME_SIZE_1536
        )
    )

    /**
     * <p>
     * Validates the sample rate for a given VAD model.
     * </p>
     * @param model The VAD model to validate.
     * @param sampleRate The sample rate to validate.
     * @throws IllegalArgumentException if the sample rate is not supported by the model.
     */
    @JvmStatic
    fun validateSampleRate(model: Model, sampleRate: SampleRate) {
        require(getValidSampleRates(model).contains(sampleRate)) {
            "${model.name} doesn't support Sample Rate:${sampleRate.value}!"
        }
    }

    /**
     * <p>
     * Validates the frame size for a given VAD model and sample rate.
     * </p>
     * @param model The VAD model to validate.
     * @param sampleRate The sample rate to validate.
     * @param frameSize The frame size to validate.
     * @throws IllegalArgumentException if the frame size is not supported by the
     * model and sample rate combination.
     */
    @JvmStatic
    fun validateFrameSize(model: Model, sampleRate: SampleRate, frameSize: FrameSize) {
        require(getValidFrameSizes(model, sampleRate).contains(frameSize)) {
            "${model.name} doesn't support Sample rate:${sampleRate.value} and Frame Size:${frameSize.value}!"
        }
    }

    /**
     * <p>
     * Returns the valid frame sizes for a given VAD model and sample rate.
     * </p>
     * @param model The VAD model to retrieve valid frame sizes for.
     * @param sampleRate The sample rate to retrieve valid frame sizes for.
     * @return The list of valid frame sizes.
     */
    @JvmStatic
    fun getValidFrameSizes(model: Model, sampleRate: SampleRate): List<FrameSize> {
        return when (model) {
            Model.WEB_RTC_GMM -> WEB_RTC_VALID_FRAMES[sampleRate] ?: emptyList()
            Model.SILERO_DNN -> SILERO_VALID_FRAMES[sampleRate] ?: emptyList()
        }
    }

    /**
     * <p>
     * Returns the valid sample rates for a given VAD model.
     * </p>
     * @param model The VAD model to retrieve valid sample rates for.
     * @return The list of valid sample rates.
     */
    @JvmStatic
    fun getValidSampleRates(model: Model): List<SampleRate> {
        return when (model) {
            Model.WEB_RTC_GMM -> WEB_RTC_VALID_FRAMES.keys.toList()
            Model.SILERO_DNN -> SILERO_VALID_FRAMES.keys.toList()
        }
    }
}