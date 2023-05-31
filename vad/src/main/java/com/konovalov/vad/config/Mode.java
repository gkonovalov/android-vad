package com.konovalov.vad.config;

public enum Mode {
    NORMAL(0),
    AGGRESSIVE(2),
    VERY_AGGRESSIVE(3);

    private final int mode;

    public int getValue() {
        return mode;
    }

    Mode(int mode) {
        this.mode = mode;
    }
}
