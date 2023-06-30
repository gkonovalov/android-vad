package com.konovalov.vad.yamnet.config

/**
 * Created by Georgiy Konovalov on 26/06/2023.
 * <p>
 * Enum class representing different Frame Size used in the Yamnet VAD algorithm.
 * </p>
 * @property value The numeric value associated with the FrameSize.
 */
enum class FrameSize(val value: Int) {
    FRAME_SIZE_243(243),
    FRAME_SIZE_487(487),
    FRAME_SIZE_731(731),
    FRAME_SIZE_975(975);
}