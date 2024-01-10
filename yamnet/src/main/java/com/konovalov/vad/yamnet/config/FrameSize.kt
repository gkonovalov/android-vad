package com.konovalov.vad.yamnet.config

/**
 * Created by Georgiy Konovalov on 6/1/2023.
 *
 * Enum class representing different frame sizes used in the Yamnet VAD algorithm.
 *
 * @property value numeric value associated with the FrameSize.
 */
enum class FrameSize(val value: Int) {
    FRAME_SIZE_243(243),
    FRAME_SIZE_487(487),
    FRAME_SIZE_731(731),
    FRAME_SIZE_975(975);
}