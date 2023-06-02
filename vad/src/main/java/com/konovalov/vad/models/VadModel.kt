package com.konovalov.vad.models

import com.konovalov.vad.VadListener
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate
import java.io.Closeable

interface VadModel : Closeable {
    val model: Model
    var sampleRate: SampleRate
    var frameSize: FrameSize
    var mode: Mode
    var speechDurationMs: Int
    var silenceDurationMs: Int
    fun isSpeech(audioData: ShortArray): Boolean
    fun setContinuousSpeechListener(audio: ShortArray, listener: VadListener)
    override fun close()
}