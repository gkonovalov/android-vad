package com.konovalov.vad.webrtc.config

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * Enum class representing different Sample Rate used in the WebRTC VAD algorithm.
 * </p>
 * @property value The numeric value associated with the SampleRate.
 */
enum class SampleRate(val value: Int) {
    SAMPLE_RATE_8K(8000),
    SAMPLE_RATE_16K(16000),
    SAMPLE_RATE_32K(32000),
    SAMPLE_RATE_48K(48000);
}