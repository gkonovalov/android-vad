package com.konovalov.vad.yamnet.config

/**
 * Created by Georgiy Konovalov on 6/1/2023.
 *
 * Enum class representing different sample rates used in the Yamnet VAD algorithm.
 *
 * @property value numeric value associated with the SampleRate.
 */
enum class SampleRate(val value: Int) {
    SAMPLE_RATE_16K(16000)
}