package com.konovalov.vad;

import com.konovalov.vad.config.FrameSize;
import com.konovalov.vad.config.Mode;
import com.konovalov.vad.config.Model;
import com.konovalov.vad.config.SampleRate;

import java.io.Closeable;

public interface Vad extends Closeable {

    Model getModel();

    SampleRate getSampleRate();

    void setSampleRate(SampleRate sampleRate);

    FrameSize getFrameSize();

    void setFrameSize(FrameSize frameSize);

    Mode getMode();

    void setMode(Mode mode);

    void setSpeechDurationMs(int speechDurationMs);

    int getSpeechDurationMs();

    void setSilenceDurationMs(int silenceDurationMs);

    int getSilenceDurationMs();

    boolean isSpeech(short[] audio);

    void setContinuousSpeechListener(short[] audio, VadListener listener);

    void close();
}
