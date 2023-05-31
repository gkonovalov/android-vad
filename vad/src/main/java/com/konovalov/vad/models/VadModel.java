package com.konovalov.vad.models;

import com.konovalov.vad.Vad;
import com.konovalov.vad.VadListener;
import com.konovalov.vad.Validator;
import com.konovalov.vad.config.FrameSize;
import com.konovalov.vad.config.Mode;
import com.konovalov.vad.config.SampleRate;

abstract class VadModel implements Vad {
    private SampleRate sampleRate;
    private FrameSize frameSize;
    private Mode mode;

    private long speechFramesCount;
    private long silenceFramesCount;
    private int maxSpeechFramesCount;
    private int maxSilenceFramesCount;

    protected VadModel(VadBuilder builder) {
        this.speechFramesCount = 0;
        this.silenceFramesCount = 0;

        setSampleRate(builder.sampleRate);
        setFrameSize(builder.frameSize);
        setMode(builder.mode);
        setSpeechDurationMs(builder.speechDurationMs);
        setSilenceDurationMs(builder.silenceDurationMs);
    }

    @Override
    public void setContinuousSpeechListener(short[] audio, VadListener listener) {
        if (listener == null) {
            throw new NullPointerException("Parameter VadListener can't be null!");
        }

        if (isSpeech(audio)) {
            silenceFramesCount = 0;
            if (++speechFramesCount > maxSpeechFramesCount) {
                speechFramesCount = 0;
                listener.onSpeechDetected();
            }
        } else {
            speechFramesCount = 0;
            if (++silenceFramesCount > maxSilenceFramesCount) {
                silenceFramesCount = 0;
                listener.onNoiseDetected();
            }
        }
    }

    @Override
    public void setSampleRate(SampleRate sampleRate) {
        Validator.validateSampleRate(getModel(), sampleRate);
        this.sampleRate = sampleRate;

    }

    @Override
    public void setFrameSize(FrameSize frameSize) {
        Validator.validateFrameSize(getModel(), getSampleRate(), frameSize);
        this.frameSize = frameSize;
    }

    public void setMode(Mode mode) {
        if (mode == null) {
            throw new NullPointerException("Parameter Mode can't be null!");
        }

        this.mode = mode;
    }

    public void setSpeechDurationMs(int speechDurationMs) {
        if (speechDurationMs < 0) {
            throw new IllegalStateException("Parameter speechDurationMs can't be below zero!");
        }

        this.maxSpeechFramesCount = getFramesCount(speechDurationMs);
    }

    public void setSilenceDurationMs(int silenceDurationMs) {
        if (silenceDurationMs < 0) {
            throw new IllegalStateException("Parameter silenceDurationMs can't be below zero!");
        }

        this.maxSilenceFramesCount = getFramesCount(silenceDurationMs);
    }

    @Override
    public SampleRate getSampleRate() {
        return sampleRate;
    }

    public int getSampleRateInt() {
        return getSampleRate().getValue();
    }

    @Override
    public FrameSize getFrameSize() {
        return frameSize;
    }

    public int getFrameSizeInt() {
        return getFrameSize().getValue();
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    public int getModeInt() {
        return getMode().getValue();
    }

    public int getSpeechDurationMs() {
        return getDurationMs(maxSpeechFramesCount);
    }

    public int getSilenceDurationMs() {
        return getDurationMs(maxSilenceFramesCount);
    }

    private int getDurationMs(int frameCount) {
        if (frameCount <= 0) {
            return 0;
        }

        int frameLengthMs = getFrameSize().getValue() / (getSampleRate().getValue() / 1000);
        return frameCount * frameLengthMs;
    }

    private int getFramesCount(int durationMs) {
        if (durationMs <= 0) {
            return 0;
        }

        int frameLengthMs = getFrameSize().getValue() / (getSampleRate().getValue() / 1000);
        return durationMs / frameLengthMs;
    }
}
