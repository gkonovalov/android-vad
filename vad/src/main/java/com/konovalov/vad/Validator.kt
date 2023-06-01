package com.konovalov.vad

import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate

object Validator {

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

    @JvmStatic
    fun validateSampleRate(model: Model, sampleRate: SampleRate) {
        require(getValidSampleRates(model).contains(sampleRate)) {
            "${model.name} doesn't support Sample Rate:${sampleRate.value}!"
        }
    }

    @JvmStatic
    fun validateFrameSize(model: Model, sampleRate: SampleRate, frameSize: FrameSize) {
        require(getValidFrameSizes(model, sampleRate).contains(frameSize)) {
            "${model.name} doesn't support Sample rate:${sampleRate.value} and Frame Size:${frameSize.value}!"
        }
    }

    @JvmStatic
    fun getValidFrameSizes(model: Model, sampleRate: SampleRate): List<FrameSize> {
        return when (model) {
            Model.WEB_RTC_GMM -> WEB_RTC_VALID_FRAMES[sampleRate] ?: emptyList()
            Model.SILERO_DNN -> SILERO_VALID_FRAMES[sampleRate] ?: emptyList()
        }
    }

    @JvmStatic
    fun getValidSampleRates(model: Model): List<SampleRate> {
        return when (model) {
            Model.WEB_RTC_GMM -> WEB_RTC_VALID_FRAMES.keys.toList()
            Model.SILERO_DNN -> SILERO_VALID_FRAMES.keys.toList()
        }
    }
}