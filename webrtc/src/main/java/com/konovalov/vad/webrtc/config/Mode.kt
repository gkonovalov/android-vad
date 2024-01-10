package com.konovalov.vad.webrtc.config

/**
 * Created by Georgiy Konovalov on 6/1/2023.
 *
 * Enum class representing different Mode used in the WebRTC VAD algorithm.
 *
 * @property value numeric value associated with the Mode.
 */
enum class Mode(val value: Int) {
    NORMAL(0),
    LOW_BITRATE(1),
    AGGRESSIVE(2),
    VERY_AGGRESSIVE(3);
}