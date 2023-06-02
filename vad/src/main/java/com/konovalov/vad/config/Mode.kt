package com.konovalov.vad.config

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * Enum class representing different detection Modes used in the VAD algorithm. Mode determine
 * the aggressiveness of the voice activity detection algorithm.
 * </p>
 * @property value The numeric value associated with the Mode.
 */
enum class Mode(val value: Int) {
    NORMAL(0),
    AGGRESSIVE(2),
    VERY_AGGRESSIVE(3);
}