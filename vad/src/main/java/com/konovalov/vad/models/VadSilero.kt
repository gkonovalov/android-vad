package com.konovalov.vad.models

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.content.Context
import com.konovalov.vad.R
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.reflect.cast

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * The Silero VAD algorithm, based on DNN, analyzes the audio signal to determine whether it
 * contains speech or non-speech segments. It offers higher accuracy in differentiating speech from
 * background noise compared to the WebRTC VAD algorithm.
 *
 * The Silero VAD supports the following parameters:
 *
 * Sample Rates: 8000Hz, 16000Hz
 *
 * Frame Sizes (per sample rate):
 *             For 8000Hz: 256, 512, 768
 *             For 16000Hz: 512, 1024, 1536
 *
 * Mode: NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 * </p>
 * @param context The context is required and helps with reading the model file from the file system.
 * @param sampleRate The sample rate of the audio input.
 * @param frameSize The frame size of the audio input.
 * @param mode The mode of the VAD model.
 * @param speechDurationMs The minimum duration in milliseconds for speech segments.
 * @param silenceDurationMs The minimum duration in milliseconds for silence segments.
 */
internal class VadSilero(
    context: Context,
    sampleRate: SampleRate,
    frameSize: FrameSize,
    mode: Mode,
    speechDurationMs: Int,
    silenceDurationMs: Int
) : VadBase(
    sampleRate,
    frameSize,
    mode,
    speechDurationMs,
    silenceDurationMs
) {

    private var session: OrtSession? = null

    private val THREADS_COUNT = 1

    private var h = FloatArray(128)
    private var c = FloatArray(128)

    /**
     * <p>
     * Initializes the ONNIX Runtime by creating a session with the provided model file and session options.
     * The session will be used for making predictions.
     * </p>
     * @param context The context required for accessing the model file.
     */
    init {
        val env = OrtEnvironment.getEnvironment()
        val sessionOptions = SessionOptions()
        sessionOptions.setIntraOpNumThreads(THREADS_COUNT)
        sessionOptions.setInterOpNumThreads(THREADS_COUNT)
        sessionOptions.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT)
        session = env.createSession(getModel(context), sessionOptions)
    }

    /**
     * <p>
     * Determines if the provided audio data contains speech based on the inference result.
     * The audio data is passed to the model for prediction. The result is obtained and compared
     * with the threshold value to determine if it represents speech.
     * </p>
     * @param audioData The audio data to analyze.
     * @return True if speech is detected, false otherwise.
     */
    override fun isSpeech(audioData: ShortArray): Boolean {
        return getResult(session?.run(getInputTensors(audioData))) > threshold()
    }

    /**
     * <p>
     * Retrieves and processes the output tensors to obtain the confidence value.
     * The output tensor contains the confidence value, as well as the updated hidden state (H)
     * and cell state (C) values. The H and C values are flattened and converted to float arrays
     * for further processing.
     * </p>
     * @param output The result of the inference session.
     * @return The confidence value.
     */
    private fun getResult(output: OrtSession.Result?): Float {
        val confidence = Array<FloatArray>::class.cast(output?.get(OutputTensors.OUTPUT)?.value)
        val hn = Array<Array<FloatArray>>::class.cast(output?.get(OutputTensors.HN)?.value)
        val cn = Array<Array<FloatArray>>::class.cast(output?.get(OutputTensors.CN)?.value)

        h = hn.flatten().flatMap { it.asIterable() }.toFloatArray()
        c = cn.flatten().flatMap { it.asIterable() }.toFloatArray()

        return confidence[0][0]
    }

    /**
     * <p>
     * Creates and returns a map of input tensors for the given audio data, Sample Rate and Frame Size.
     * The audio data is converted to a float array and wrapped in an OnnxTensor with the
     * corresponding tensor shape. The sample rate, hidden state (H), and cell state (C) tensors
     * are also created and added to the map.
     * </p>
     * @param audioData The audio data as a ShortArray.
     * @return A map of input tensors as a Map<String, OnnxTensor>.
     * @throws OrtException If there was an error in creating the
     * tensors or obtaining the OrtEnvironment.
     */
    private fun getInputTensors(audioData: ShortArray): Map<String, OnnxTensor> {
        val env = OrtEnvironment.getEnvironment()

        return mapOf(
            InputTensors.INPUT to OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(audioData.map { it / 32768.0f }.toFloatArray()),
                longArrayOf(1, frameSize.value.toLong())
            ),
            InputTensors.SR to OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(longArrayOf(sampleRate.value.toLong())),
                longArrayOf(1)
            ),
            InputTensors.H to OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(h),
                longArrayOf(2, 1, 64)
            ),
            InputTensors.C to OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(c),
                longArrayOf(2, 1, 64)
            )
        )
    }

    /**
     * <p>
     * Retrieves the model data as a byte array from R.raw.silero_vad.
     * </p>
     * @param context The android application context.
     * @return The model data as a ByteArray.
     */
    private fun getModel(context: Context): ByteArray {
        return context.resources.openRawResource(R.raw.silero_vad).readBytes()
    }

    /**
     * <p>
     * Calculates and returns the threshold value based on the value of detection mode.
     * The threshold value represents the confidence level required for VAD to make proper decision.
     * ex. Mode.VERY_AGGRESSIVE requiring a very high prediction accuracy from the model.
     * </p>
     * @return The threshold Float value.
     */
    private fun threshold(): Float = when (mode) {
        Mode.AGGRESSIVE -> 0.8f
        Mode.VERY_AGGRESSIVE -> 0.95f
        else -> 0.5f
    }

    /**
     * <p>
     * Return current model type.
     * </p>
     * @return {@code Model.SILERO_DNN}
     */
    override val model: Model
        get() = Model.SILERO_DNN

    /**
     * <p>
     * Closes the ONNX Session and releases all associated resources.
     * This method should be called when the VAD is no longer needed to free up system resources.
     * </p>
     */
    override fun close() {
        session?.close()
        session = null
    }

    /**
     * <p>
     * Constants representing the input tensor names used during model prediction.
     * </p>
     */
    object InputTensors {
        const val INPUT = "input"
        const val SR = "sr"
        const val H = "h"
        const val C = "c"
    }

    /**
     * <p>
     * Constants representing the output tensor names used when the model returns a result.
     * </p>
     */
    object OutputTensors {
        const val OUTPUT = 0
        const val HN = 1
        const val CN = 2
    }
}