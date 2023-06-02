package com.konovalov.vad

import android.content.Context
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate
import com.konovalov.vad.models.VadModel
import com.konovalov.vad.models.VadSilero
import com.konovalov.vad.models.VadWebRTC

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