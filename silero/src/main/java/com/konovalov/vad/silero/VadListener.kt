package com.konovalov.vad.silero

/**
 * Created by Georgiy Konovalov on 6/1/2023.
 *
 * Interface representing a listener for Voice Activity Detection (VAD) events.
 */
interface VadListener {
    fun onSpeechDetected()
    fun onNoiseDetected()
}