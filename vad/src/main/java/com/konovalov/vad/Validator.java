package com.konovalov.vad;

import com.konovalov.vad.config.FrameSize;
import com.konovalov.vad.config.Model;
import com.konovalov.vad.config.SampleRate;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

final public class Validator {

    private static final LinkedHashMap<SampleRate, LinkedList<FrameSize>> WEB_RTC_VALID_FRAMES =
            new LinkedHashMap<SampleRate, LinkedList<FrameSize>>() {{
                //WEB_RTC supports only 10, 20 or 30 ms frames.
                put(SampleRate.SAMPLE_RATE_8K, new LinkedList<FrameSize>() {{
                    add(FrameSize.FRAME_SIZE_80);
                    add(FrameSize.FRAME_SIZE_160);
                    add(FrameSize.FRAME_SIZE_240);
                }});
                put(SampleRate.SAMPLE_RATE_16K, new LinkedList<FrameSize>() {{
                    add(FrameSize.FRAME_SIZE_160);
                    add(FrameSize.FRAME_SIZE_320);
                    add(FrameSize.FRAME_SIZE_480);
                }});
                put(SampleRate.SAMPLE_RATE_32K, new LinkedList<FrameSize>() {{
                    add(FrameSize.FRAME_SIZE_320);
                    add(FrameSize.FRAME_SIZE_640);
                    add(FrameSize.FRAME_SIZE_960);
                }});
                put(SampleRate.SAMPLE_RATE_48K, new LinkedList<FrameSize>() {{
                    add(FrameSize.FRAME_SIZE_480);
                    add(FrameSize.FRAME_SIZE_960);
                    add(FrameSize.FRAME_SIZE_1440);
                }});
            }};

    private static final LinkedHashMap<SampleRate, LinkedList<FrameSize>> SILERO_VALID_FRAMES =
            new LinkedHashMap<SampleRate, LinkedList<FrameSize>>() {{
                //SILERO supports only 32, 64 or 96 ms frames.
                //Valid frame size: 256 512 768 for 8k; 512 1024 1536 for 16k.
                put(SampleRate.SAMPLE_RATE_8K, new LinkedList<FrameSize>() {{
                    add(FrameSize.FRAME_SIZE_256);
                    add(FrameSize.FRAME_SIZE_512);
                    add(FrameSize.FRAME_SIZE_768);
                }});
                put(SampleRate.SAMPLE_RATE_16K, new LinkedList<FrameSize>() {{
                    add(FrameSize.FRAME_SIZE_512);
                    add(FrameSize.FRAME_SIZE_1024);
                    add(FrameSize.FRAME_SIZE_1536);
                }});
            }};

    public static void validateSampleRate(Model model, SampleRate sampleRate) {
        if (sampleRate == null) {
            throw new NullPointerException("Non-null SampleRate required!");
        }

        if (!getValidSampleRates(model).contains(sampleRate)) {
            throw new IllegalArgumentException(model.name() + " doesn't support Sample Rate: " +
                    sampleRate.getValue() + "!");
        }
    }

    public static void validateFrameSize(Model model, SampleRate sampleRate, FrameSize frameSize) {
        if (frameSize == null) {
            throw new NullPointerException("Non-null FrameSize required!");
        }

        if (!getValidFrameSizes(model, sampleRate).contains(frameSize)) {
            throw new IllegalArgumentException(model.name() + " doesn't support Frame Size: " +
                    frameSize.getValue() + "!");
        }
    }

    public static List<FrameSize> getValidFrameSizes(Model model, SampleRate sampleRate) {
        if (model == null) {
            throw new NullPointerException("Non-null Model required!");
        }

        if (sampleRate == null) {
            throw new NullPointerException("Non-null SampleRate required!");
        }

        switch (model) {
            case WEB_RTC_GMM:
                return new LinkedList<>(Objects.requireNonNull(WEB_RTC_VALID_FRAMES.get(sampleRate)));
            case SILERO_DNN:
                return new LinkedList<>(Objects.requireNonNull(SILERO_VALID_FRAMES.get(sampleRate)));
            default:
                throw new IllegalArgumentException("Model is incorrect!");
        }
    }

    public static List<SampleRate> getValidSampleRates(Model model) {
        if (model == null) {
            throw new NullPointerException("Non-null Model required!");
        }

        switch (model) {
            case WEB_RTC_GMM:
                return new LinkedList<>(WEB_RTC_VALID_FRAMES.keySet());
            case SILERO_DNN:
                return new LinkedList<>(SILERO_VALID_FRAMES.keySet());
            default:
                throw new IllegalArgumentException("Model is incorrect!");
        }
    }
}
