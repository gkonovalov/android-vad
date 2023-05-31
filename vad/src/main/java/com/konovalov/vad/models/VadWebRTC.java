package com.konovalov.vad.models;

import com.konovalov.vad.config.Mode;
import com.konovalov.vad.config.Model;

/**
 * Created by Georgiy Konovalov on 5/22/2023.
 */
final class VadWebRTC extends VadModel {

    public VadWebRTC(VadBuilder builder) {
        super(builder);
        init();
    }

    private void init() {
        try {
            if (!nativeInit()) {
                throw new RuntimeException("Error can't init WebRTC VAD!");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error nativeInit failed!", e);
        }

        updateDetectionMode();
    }

    /**
     * Speech detector was designed to detect speech/noise in small audio
     * frames and return result for every frame. This method will not work
     * for long utterances.
     *
     * @param audioData input audio frame
     * @return boolean containing result of speech detection
     */
    @Override
    public boolean isSpeech(short[] audioData) {
        if (audioData == null) {
            return false;
        }

        try {
            return nativeIsSpeech(getSampleRateInt(), getFrameSizeInt(), audioData);
        } catch (Exception e) {
            throw new RuntimeException("Error nativeIsSpeech failed!", e);
        }
    }

    @Override
    public void close() {
        try {
            nativeDestroy();
        } catch (Exception e) {
            throw new RuntimeException("Error nativeDestroy failed!", e);
        }
    }

    @Override
    public void setMode(Mode mode) {
        super.setMode(mode);
        updateDetectionMode();
    }

    private void updateDetectionMode() {
        try {
            nativeSetMode(getModeInt());
        } catch (Exception e) {
            throw new RuntimeException("Error nativeSetMode failed!", e);
        }
    }

    @Override
    public Model getModel() {
        return Model.WEB_RTC_GMM;
    }

    private native boolean nativeInit();

    private native boolean nativeSetMode(int mode);

    private native boolean nativeIsSpeech(int sampleRate, int frameSize, short[] audio);

    private native void nativeDestroy();

    static {
        System.loadLibrary("vad_jni");
    }
}
