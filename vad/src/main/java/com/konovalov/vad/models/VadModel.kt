package com.konovalov.vad.models

import com.konovalov.vad.VadListener
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate
import java.io.Closeable

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * The code defines an interface that represents a VAD model.
 * It extends the Closeable interface to ensure proper resource cleanup. It provides properties
 * for the VAD model, sample rate, frame size, mode, speech duration, and silence duration.
 * It also defines methods for detecting speech, setting a continuous speech listener,
 * and closing the VAD model.
 * </p>
 */
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