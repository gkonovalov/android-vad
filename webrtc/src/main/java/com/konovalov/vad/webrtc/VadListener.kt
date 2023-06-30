package com.konovalov.vad.webrtc

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * Interface representing a listener for Voice Activity Detection (VAD) events.
 * </p>
 */
interface VadListener {
    fun onSpeechDetected()
    fun onNoiseDetected()
}