package com.konovalov.vad;

import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Created by George Konovalov on 11/16/2019.
 */

public class Vad {

    private VadConfig config;

    private boolean needResetDetectedSamples = true;
    private long detectedVoiceSamplesMillis = 0;
    private long detectedSilenceSamplesMillis = 0;
    private long previousTimeMillis = System.currentTimeMillis();

    public static final LinkedHashMap<VadConfig.SampleRate, LinkedList<VadConfig.FrameSize>> SAMPLE_RATE_VALID_FRAMES = new LinkedHashMap<VadConfig.SampleRate, LinkedList<VadConfig.FrameSize>>() {{
        put(VadConfig.SampleRate.SAMPLE_RATE_8K, new LinkedList<VadConfig.FrameSize>() {{
            add(VadConfig.FrameSize.FRAME_SIZE_80);
            add(VadConfig.FrameSize.FRAME_SIZE_160);
            add(VadConfig.FrameSize.FRAME_SIZE_240);
        }});
        put(VadConfig.SampleRate.SAMPLE_RATE_16K, new LinkedList<VadConfig.FrameSize>() {{
            add(VadConfig.FrameSize.FRAME_SIZE_160);
            add(VadConfig.FrameSize.FRAME_SIZE_320);
            add(VadConfig.FrameSize.FRAME_SIZE_480);
        }});
        put(VadConfig.SampleRate.SAMPLE_RATE_32K, new LinkedList<VadConfig.FrameSize>() {{
            add(VadConfig.FrameSize.FRAME_SIZE_320);
            add(VadConfig.FrameSize.FRAME_SIZE_640);
            add(VadConfig.FrameSize.FRAME_SIZE_960);
        }});
        put(VadConfig.SampleRate.SAMPLE_RATE_48K, new LinkedList<VadConfig.FrameSize>() {{
            add(VadConfig.FrameSize.FRAME_SIZE_480);
            add(VadConfig.FrameSize.FRAME_SIZE_960);
            add(VadConfig.FrameSize.FRAME_SIZE_1440);
        }});
    }};

    public Vad() {
    }

    public Vad(VadConfig config) {
        this.config = config;
    }

    public void start() {
        if (config == null) {
            throw new NullPointerException("VadConfig is NULL!");
        }

        if (!isSampleRateAndFrameSizeValid()) {
            throw new UnsupportedOperationException("VAD doesn't support this SampleRate and FrameSize!");
        }

        try {
            nativeStart(config.getSampleRate().getValue(), config.getFrameSize().getValue(), config.getMode().getValue());
        } catch (Exception e) {
            throw new RuntimeException("Error can't start VAD!", e);
        }
    }

    public void stop() {
        try {
            nativeStop();
        } catch (Exception e) {
            throw new RuntimeException("Error can't stop VAD!", e);
        }
    }

    public boolean isSpeech(short[] audio) {
        if (audio == null) {
            throw new NullPointerException("Audio data is NULL!");
        }

        try {
            return nativeIsSpeech(audio);
        } catch (Exception e) {
            throw new RuntimeException("Error during VAD speech detection!", e);
        }
    }

    public void isContinuousSpeech(short[] audio, VadListener listener) {
        if (audio == null) {
            throw new NullPointerException("Audio data is NULL!");
        }

        if (listener == null) {
            throw new NullPointerException("VadListener is NULL!");
        }

        if (config == null) {
            throw new NullPointerException("VadConfig is NULL!");
        }

        long currentTimeMillis = System.currentTimeMillis();

        if (isSpeech(audio)) {
            detectedVoiceSamplesMillis += currentTimeMillis - previousTimeMillis;
            needResetDetectedSamples = true;
            if (detectedVoiceSamplesMillis > config.getVoiceDurationMillis()) {
                previousTimeMillis = currentTimeMillis;
                listener.onSpeechDetected();
            }
        } else {
            if (needResetDetectedSamples) {
                needResetDetectedSamples = false;
                detectedSilenceSamplesMillis = 0;
                detectedVoiceSamplesMillis = 0;
            }
            detectedSilenceSamplesMillis += currentTimeMillis - previousTimeMillis;
            if (detectedSilenceSamplesMillis > config.getSilenceDurationMillis()) {
                previousTimeMillis = currentTimeMillis;
                listener.onNoiseDetected();
            }
        }
        previousTimeMillis = currentTimeMillis;
    }

    public VadConfig getConfig() {
        return config;
    }

    public void setConfig(VadConfig config) {
        this.config = config;
    }

    private boolean isSampleRateAndFrameSizeValid() {
        if (config == null) {
            throw new NullPointerException("VadConfig is NULL!");
        }

        LinkedList<VadConfig.FrameSize> supportingFrameSizes = getValidFrameSize(config.getSampleRate());

        if (supportingFrameSizes != null) {
            return supportingFrameSizes.contains(config.getFrameSize());
        } else {
            return false;
        }
    }

    public static LinkedList<VadConfig.FrameSize> getValidFrameSize(VadConfig.SampleRate sampleRate) {
        if (sampleRate == null) {
            throw new NullPointerException("SampleRate is NULL!");
        }

        return SAMPLE_RATE_VALID_FRAMES.get(sampleRate);
    }

    private native int nativeStart(int sampleRate, int frameSize, int mode);

    private native boolean nativeIsSpeech(short[] audio);

    private native void nativeStop();

    static {
        System.loadLibrary("vad_jni");
    }
}
