package com.konovalov.vad.models;

import com.konovalov.vad.config.Mode;
import com.konovalov.vad.config.Model;

/**
 * Created by Georgiy Konovalov on 5/22/2023.
 */
final class VadWebRTC extends VadModel {

    private long nativeHandle;

    public VadWebRTC(VadBuilder builder) {
        super(builder);
        init();
    }

    private void init() {
        this.nativeHandle = nativeInit();

        if (nativeHandle <= 0) {
            throw new RuntimeException("Error can't init WebRTC VAD!");
        }

        setMode();
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
        if (audioData == null && !isVadInitialized()) {
            return false;
        }

        return nativeIsSpeech(nativeHandle, getSampleRateInt(), getFrameSizeInt(), audioData);
    }

    @Override
    public void close() {
        if (!isVadInitialized()) {
            return;
        }

        nativeDestroy(nativeHandle);
        nativeHandle = -1;
    }

    private void setMode() {
        if (!isVadInitialized()) {
            return;
        }

        nativeSetMode(nativeHandle, getModeInt());
    }

    @Override
    public void setMode(Mode mode) {
        super.setMode(mode);
        setMode();
    }

    @Override
    public Model getModel() {
        return Model.WEB_RTC_GMM;
    }

    private boolean isVadInitialized() {
        return nativeHandle > 0;
    }

    private native long nativeInit();

    private native boolean nativeSetMode(long nativeHandle, int mode);

    private native boolean nativeIsSpeech(long nativeHandleint, int sampleRate, int frameSize, short[] audio);

    private native void nativeDestroy(long nativeHandle);

    static {
        System.loadLibrary("vad_jni");
    }
}
