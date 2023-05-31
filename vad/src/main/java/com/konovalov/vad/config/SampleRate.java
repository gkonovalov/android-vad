package com.konovalov.vad.config;

public enum SampleRate {
    SAMPLE_RATE_8K(8000),
    SAMPLE_RATE_16K(16000),
    SAMPLE_RATE_32K(32000),
    SAMPLE_RATE_48K(48000);

    private final int sampleRate;

    public int getValue() {
        return sampleRate;
    }

    SampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }
}
