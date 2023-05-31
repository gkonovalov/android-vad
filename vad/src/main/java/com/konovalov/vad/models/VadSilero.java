package com.konovalov.vad.models;

import android.content.Context;

import com.konovalov.vad.config.Model;
import com.konovalov.vad.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

/**
 * Created by Georgiy Konovalov on 5/22/2023.
 */
final class VadSilero extends VadModel {

    private OrtSession session;

    private static final int BATCH = 1;
    private static final int THREADS_COUNT = 1;
    private float[] h = new float[2 * BATCH * 64];
    private float[] c = new float[2 * BATCH * 64];

    public VadSilero(Context context, VadBuilder builder) {
        super(builder);
        init(context);
    }

    private void init(Context context) {
        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            sessionOptions.setIntraOpNumThreads(THREADS_COUNT);
            sessionOptions.setInterOpNumThreads(THREADS_COUNT);
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

            this.session = env.createSession(getModel(context), sessionOptions);
        } catch (OrtException e) {
            throw new RuntimeException("Can't init ONNX!", e);
        }
    }

    @Override
    public boolean isSpeech(short[] audioData) {
        if (audioData == null) {
            return false;
        }

        try (OrtSession.Result result = session.run(getInputTensors(shortToFloat(audioData)))) {
            return getResult(result) > getThreshold();
        } catch (OrtException e) {
            throw new RuntimeException("Tensors processing error!", e);
        } catch (NullPointerException e) {
            throw new RuntimeException("You cannot detect speech after closing the ONNX Session. " +
                    "Please build the SILERO_DNN VAD again!", e);
        }
    }

    private float getResult(OrtSession.Result result) throws OrtException {
        float[][] output = new float[1][1];

        for (Map.Entry<String, OnnxValue> item : result) {
            OnnxValue onnxValue = item.getValue();

            switch (item.getKey()) {
                case OutputTensors.OUTPUT:
                    output = (float[][]) onnxValue.getValue();
                    break;
                case OutputTensors.HN:
                    concatArrays((float[][][]) onnxValue.getValue(), h);
                    break;
                case OutputTensors.CN:
                    concatArrays((float[][][]) onnxValue.getValue(), c);
                    break;
            }
        }

        return output[0][0];
    }

    private LinkedHashMap<String, OnnxTensor> getInputTensors(float[] audioData) throws OrtException {
        OrtEnvironment env = OrtEnvironment.getEnvironment();

        return new LinkedHashMap<String, OnnxTensor>() {{
            put(InputTensors.INPUT, OnnxTensor.createTensor(env,
                    FloatBuffer.wrap(audioData),
                    makeArray(1, getFrameSizeInt())
            ));
            put(InputTensors.SR, OnnxTensor.createTensor(env,
                    LongBuffer.wrap(makeArray(getSampleRateInt())),
                    makeArray(1)
            ));
            put(InputTensors.H, OnnxTensor.createTensor(env,
                    FloatBuffer.wrap(h),
                    makeArray(2, BATCH, 64)
            ));
            put(InputTensors.C, OnnxTensor.createTensor(env,
                    FloatBuffer.wrap(c),
                    makeArray(2, BATCH, 64)
            ));
        }};
    }

    private byte[] getModel(Context context) {
        try (InputStream is = context.getResources().openRawResource(R.raw.silero_vad)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buff = new byte[is.available()];
            int bytesRead;

            while ((bytesRead = is.read(buff, 0, buff.length)) > 0) {
                outputStream.write(buff, 0, bytesRead);
            }

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Can't read ONNX model file!", e);
        }
    }

    private float getThreshold() {
        switch (getMode()) {
            case AGGRESSIVE:
                return 0.8f;
            case VERY_AGGRESSIVE:
                return 0.95f;
            default:
                return 0.5f;
        }
    }

    @Override
    public Model getModel() {
        return Model.SILERO_DNN;
    }

    @Override
    public void close() {
        reset();

        try {
            if (session != null) {
                session.close();
                session = null;
            }
        } catch (OrtException e) {
            throw new RuntimeException("Closing ONNX session Error!", e);
        }
    }

    private float[] shortToFloat(short[] audio) {
        float[] audioFloats = new float[audio.length];

        for (int i = 0; i < audio.length; i++) {
            audioFloats[i] = audio[i] / 32768.0f;
        }

        return audioFloats;
    }

    private void concatArrays(float[][][] fromArr, float[] toArr) {
        int offset = 0;
        for (float[][] items : fromArr) {
            for (float[] item : items) {
                System.arraycopy(item, 0, toArr, offset, item.length);
                offset += item.length;
            }
        }
    }

    private long[] makeArray(long... dim) {
        return dim;
    }

    private void reset() {
        this.h = new float[2 * BATCH * 64];
        this.c = new float[2 * BATCH * 64];
    }

    interface InputTensors {
        String INPUT = "input";
        String SR = "sr";
        String H = "h";
        String C = "c";
    }

    interface OutputTensors {
        String OUTPUT = "output";
        String HN = "hn";
        String CN = "cn";
    }
}
