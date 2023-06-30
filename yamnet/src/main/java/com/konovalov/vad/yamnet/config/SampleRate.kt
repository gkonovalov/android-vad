package com.konovalov.vad.yamnet.config

/**
 * Created by Georgiy Konovalov on 26/06/2023.
 * <p>
 * Enum class representing different Sample Rates used in the Yamnet VAD algorithm.
 * </p>
 * @property value The numeric value associated with the SampleRate.
 */
enum class SampleRate(val value: Int) {
    SAMPLE_RATE_16K(16000)
}