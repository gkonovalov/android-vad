package com.konovalov.vad.silero.config

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * Enum class representing different Frame Sizes used in the VAD algorithm.
 * </p>
 * @property value The numeric value associated with the FrameSize.
 */
enum class FrameSize(val value: Int) {
    FRAME_SIZE_256(256),
    FRAME_SIZE_512(512),
    FRAME_SIZE_768(768),
    FRAME_SIZE_1024(1024),
    FRAME_SIZE_1536(1536);
}