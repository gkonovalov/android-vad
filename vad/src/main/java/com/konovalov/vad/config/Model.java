package com.konovalov.vad.config;

public enum Model {
    WEB_RTC_GMM(0),
    SILERO_DNN(1);

    private final int model;

    public int getValue() {
        return model;
    }

    Model(int model) {
        this.model = model;
    }
}
