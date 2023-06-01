package com.konovalov.vad.models

import android.content.Context
import com.konovalov.vad.Vad
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate

class VadBuilder private constructor() {
    private var context: Context? = null
    private lateinit var model: Model
    private lateinit var sampleRate: SampleRate
    private lateinit var frameSize: FrameSize
    private lateinit var mode: Mode
    private var speechDurationMs = 0
    private var silenceDurationMs = 0

    fun setModel(model: Model): VadBuilder = apply {
        this.model = model
    }

    fun setContext(context: Context): VadBuilder = apply {
        this.context = context.applicationContext ?: context
    }

    fun setSampleRate(sampleRate: SampleRate): VadBuilder = apply {
        this.sampleRate = sampleRate
    }

    fun setFrameSize(frameSize: FrameSize): VadBuilder = apply {
        this.frameSize = frameSize
    }

    fun setMode(mode: Mode): VadBuilder = apply {
        this.mode = mode
    }

    fun setSpeechDurationMs(speechDurationMs: Int): VadBuilder = apply {
        this.speechDurationMs = speechDurationMs
    }

    fun setSilenceDurationMs(silenceDurationMs: Int): VadBuilder = apply {
        this.silenceDurationMs = silenceDurationMs
    }

    fun build(): Vad {
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
        fun modelBuilder(): VadBuilder {
            return VadBuilder()
        }
    }
}