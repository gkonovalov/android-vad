package com.konovalov.vad.models;

import android.content.Context;

import com.konovalov.vad.Vad;
import com.konovalov.vad.config.FrameSize;
import com.konovalov.vad.config.Mode;
import com.konovalov.vad.config.Model;
import com.konovalov.vad.config.SampleRate;

public final class VadBuilder {

    private Context context;
    private Model model;
    SampleRate sampleRate;
    FrameSize frameSize;
    Mode mode;
    int speechDurationMs;
    int silenceDurationMs;

    private VadBuilder() {}

    public static VadBuilder newBuilder() {
        return new VadBuilder();
    }

    public VadBuilder setContext(Context context) {
        if (context.getApplicationContext() != null) {
            this.context = context.getApplicationContext();
        } else {
            this.context = context;
        }
        return this;
    }

    public VadBuilder setModel(Model model) {
        this.model = model;
        return this;
    }

    public VadBuilder setSampleRate(SampleRate sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    public VadBuilder setFrameSize(FrameSize frameSize) {
        this.frameSize = frameSize;
        return this;
    }

    public VadBuilder setMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    public VadBuilder setSpeechDurationMs(int speechDurationMs) {
        this.speechDurationMs = speechDurationMs;
        return this;
    }

    public VadBuilder setSilenceDurationMs(int silenceDurationMs) {
        this.silenceDurationMs = silenceDurationMs;
        return this;
    }

    public Vad build() {
        if (model == null) {
            throw new NullPointerException("Non-null Model required!");
        }

        if (this.context == null && model == Model.SILERO_DNN) {
            throw new IllegalArgumentException("Context is required for Model.SILERO_DNN!");
        }

        return createModel();
    }


    private Vad createModel() {
        switch (model) {
            case WEB_RTC_GMM:
                return new VadWebRTC(this);
            case SILERO_DNN:
                return new VadSilero(context, this);
            default:
                throw new IllegalArgumentException("Model is incorrect!");
        }
    }
}
