package com.konovalov.vad.yamnet.config

/**
 * Created by Georgiy Konovalov on 6/1/2023.
 *
 * Enum class representing different mode used in the Yamnet VAD algorithm.
 *
 * @property value numeric value associated with the Mode.
 */
enum class Mode(val value: Int) {
    OFF(0),
    NORMAL(1),
    AGGRESSIVE(2),
    VERY_AGGRESSIVE(3);
}