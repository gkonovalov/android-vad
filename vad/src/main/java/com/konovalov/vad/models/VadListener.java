package com.konovalov.vad.models;

/**
 * Created by George Konovalov on 11/16/2019.
 */
public interface VadListener {
    void onSpeechDetected();

    void onNoiseDetected();
}
