package com.konovalov.vad.webrtc.config

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * Enum class representing different Mode used in the WebRTC VAD algorithm.
 * </p>
 * @property value The numeric value associated with the Mode.
 */
enum class Mode(val value: Int) {
    NORMAL(0),
    LOW_BITRATE(1),
    AGGRESSIVE(2),
    VERY_AGGRESSIVE(3);
}