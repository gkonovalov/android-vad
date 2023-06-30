package com.konovalov.vad.webrtc.config

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * Enum class representing different frame sizes used in the WebRTC VAD algorithm.
 * </p>
 * @property value The numeric value associated with the FrameSize.
 */
enum class FrameSize(val value: Int) {
    FRAME_SIZE_80(80),
    FRAME_SIZE_160(160),
    FRAME_SIZE_240(240),
    FRAME_SIZE_320(320),
    FRAME_SIZE_480(480),
    FRAME_SIZE_640(640),
    FRAME_SIZE_960(960),
    FRAME_SIZE_1440(1440);
}