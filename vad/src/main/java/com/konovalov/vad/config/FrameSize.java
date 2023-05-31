package com.konovalov.vad.config;

public enum FrameSize {
    FRAME_SIZE_80(80),
    FRAME_SIZE_160(160),
    FRAME_SIZE_240(240),
    FRAME_SIZE_256(256),
    FRAME_SIZE_320(320),
    FRAME_SIZE_480(480),
    FRAME_SIZE_512(512),
    FRAME_SIZE_640(640),
    FRAME_SIZE_768(768),
    FRAME_SIZE_960(960),
    FRAME_SIZE_1024(1024),
    FRAME_SIZE_1440(1440),
    FRAME_SIZE_1536(1536);

    private final int frameSize;

    public int getValue() {
        return frameSize;
    }

    FrameSize(int frameSize) {
        this.frameSize = frameSize;
    }
}
