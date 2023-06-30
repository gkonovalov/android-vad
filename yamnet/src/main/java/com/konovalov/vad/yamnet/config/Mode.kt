package com.konovalov.vad.yamnet.config

/**
 * Created by Georgiy Konovalov on 26/06/2023.
 * <p>
 * Enum class representing different Modes used in the Yamnet VAD algorithm.
 * </p>
 * @property value The numeric value associated with the Mode.
 */
enum class Mode(val value: Int) {
    OFF(0),
    NORMAL(1),
    AGGRESSIVE(2),
    VERY_AGGRESSIVE(3);
}