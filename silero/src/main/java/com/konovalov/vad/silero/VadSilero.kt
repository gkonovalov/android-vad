package com.konovalov.vad.silero

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.content.Context
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
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
 * Mode: OFF, NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
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
class VadSilero(
    context: Context,
    sampleRate: SampleRate,
    frameSize: FrameSize,
    mode: Mode,
    speechDurationMs: Int = 0,
    silenceDurationMs: Int = 0
) {

    /**
     * <p>
     * Valid Sample Rates and Frame Sizes for Silero VAD DNN model.
     * </p>
     */
    var supportedParameters: Map<SampleRate, Set<FrameSize>> = mapOf(
        SampleRate.SAMPLE_RATE_8K to setOf(
            FrameSize.FRAME_SIZE_256,
            FrameSize.FRAME_SIZE_512,
            FrameSize.FRAME_SIZE_768
        ),
        SampleRate.SAMPLE_RATE_16K to setOf(
            FrameSize.FRAME_SIZE_512,
            FrameSize.FRAME_SIZE_1024,
            FrameSize.FRAME_SIZE_1536
        )
    )
        private set

    /**
     * Status of VAD initialization.
     */
    private var isInitiated: Boolean = false

    private val session: OrtSession

    private var h = FloatArray(128)
    private var c = FloatArray(128)

    private var speechFramesCount = 0
    private var silenceFramesCount = 0
    private var maxSpeechFramesCount = 0
    private var maxSilenceFramesCount = 0

    /**
     * <p>
     * Set, retrieve and validate speechDurationMs for Vad Model.
     * The value of this parameter will define the necessary and sufficient duration of positive
     * results to recognize result as speech. Negative numbers are not allowed.
     * </p>
     * @param speechDurationMs The speech duration ms as a Int.
     * @throws IllegalArgumentException if there was negative numbers.
     */
    var speechDurationMs: Int = speechDurationMs
        set(speechDurationMs) {
            require(speechDurationMs >= 0) {
                "Parameter speechDurationMs can't be below zero!"
            }

            field = speechDurationMs
            maxSpeechFramesCount = getFramesCount(speechDurationMs)
        }

    /**
     * <p>
     * Set, retrieve and validate silenceDurationMs for Vad Model.
     * The value of this parameter will define the necessary and sufficient duration of
     * negative results to recognize it as silence. Negative numbers are not allowed.
     * </p>
     * @param silenceDurationMs The silence duration ms as a Int.
     * @throws IllegalArgumentException if there was negative numbers.
     */
    var silenceDurationMs: Int = silenceDurationMs
        set(silenceDurationMs) {
            require(silenceDurationMs >= 0) {
                "Parameter silenceDurationMs can't be below zero!"
            }

            field = silenceDurationMs
            maxSilenceFramesCount = getFramesCount(silenceDurationMs)
        }

    /**
     * <p>
     * Set, retrieve and validate sample rate for Vad Model.
     * </p>
     * @param sampleRate The sample rate as a SampleRate.
     * @throws IllegalArgumentException if there was invalid sample rate.
     */
    var sampleRate: SampleRate = sampleRate
        set(sampleRate) {
            require(supportedParameters.containsKey(sampleRate)) {
                "VAD doesn't support Sample Rate:${sampleRate}!"
            }
            field = sampleRate
        }

    /**
     * <p>
     * Set, retrieve and validate frame size for Vad Model.
     * </p>
     * @param frameSize The sample rate as a FrameSize.
     * @throws IllegalArgumentException if there was invalid frame size.
     */
    var frameSize: FrameSize = frameSize
        set(frameSize) {
            require(supportedParameters.containsKey(sampleRate) &&
                    supportedParameters.get(sampleRate)?.contains(frameSize)!!) {
                "VAD doesn't support Sample rate:${sampleRate} and Frame Size:${frameSize}!"
            }
            field = frameSize
        }

    /**
     * <p>
     * Set and retrieve mode for Vad Model.
     * </p>
     * @param mode The sample rate as a Mode.
     */
    var mode: Mode = mode
        set(mode) {
            field = mode
        }

    /**
     * <p>
     * Initializes the ONNIX Runtime by creating a session with the provided
     * model file and session options.
     * </p>
     * @param context The context required for accessing the model file.
     */
    init {
        val env = OrtEnvironment.getEnvironment()
        val sessionOptions = SessionOptions()
        sessionOptions.setIntraOpNumThreads(1)
        sessionOptions.setInterOpNumThreads(1)
        sessionOptions.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT)
        session = env.createSession(getModel(context), sessionOptions)
        this.sampleRate = sampleRate
        this.frameSize = frameSize
        this.mode = mode
        this.silenceDurationMs = silenceDurationMs
        this.speechDurationMs = speechDurationMs
        isInitiated = true
    }

    /**
     * <p>
     * Determines if the provided audio data contains speech based on the inference result.
     * The audio data is passed to the model for prediction. The result is obtained and compared
     * with the threshold value to determine if it represents speech.
     * </p>
     * @param audioData The audio data to analyze.
     * @return True if speech is detected, False otherwise.
     */
    fun isSpeech(audioData: ShortArray): Boolean {
        checkState()
        return getResult(session.run(getInputTensors(audioData))) > threshold()
    }

    /**
     * <p>
     * Continuous Speech listener was designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * </p>
     * @param audio The audio data as a ShortArray.
     * @param listener The listener to be notified when speech or noise is detected.
     */
    fun setContinuousSpeechListener(audio: ShortArray, listener: VadListener) {
        if (isSpeech(audio)) {
            silenceFramesCount = 0
            if (++speechFramesCount > maxSpeechFramesCount) {
                speechFramesCount = 0
                listener.onSpeechDetected()
            }
        } else {
            speechFramesCount = 0
            if (++silenceFramesCount > maxSilenceFramesCount) {
                silenceFramesCount = 0
                listener.onNoiseDetected()
            }
        }
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
                FloatBuffer.wrap(audioData.map { it / 32767.0f }.toFloatArray()),
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
     * Retrieves the model data as a byte array from silero_vad.onnx.
     * </p>
     * @param context The android application context.
     * @return The model data as a ByteArray.
     */
    private fun getModel(context: Context): ByteArray {
        return context.assets.open("silero_vad.onnx").readBytes()
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
        Mode.NORMAL -> 0.5f
        Mode.AGGRESSIVE -> 0.8f
        Mode.VERY_AGGRESSIVE -> 0.95f
        else -> 0f
    }

    /**
     * <p>
     * Closes the ONNX Session and releases all associated resources.
     * This method should be called when the VAD is no longer needed to free up system resources.
     * </p>
     */
    fun close() {
        checkState()
        isInitiated = false
        session.close()
    }

    /**
     * <p>
     * Calculates the frame count based on the duration in milliseconds, frequency and frame size.
     * </p>
     * @param durationMs The duration in milliseconds.
     * @return The frame count.
     */
    private fun getFramesCount(durationMs: Int): Int {
        return durationMs / (frameSize.value / (sampleRate.value / 1000))
    }

    /**
     * <p>
     * Check if VAD session already closed.
     * </p>
     * @throws IllegalArgumentException if session already closed.
     */
    private fun checkState() {
        require(isInitiated) { "You can't use Vad after closing session!"  }
    }

    /**
     * <p>
     * Constants representing the input tensor names used during model prediction.
     * </p>
     */
    private object InputTensors {
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
    private object OutputTensors {
        const val OUTPUT = 0
        const val HN = 1
        const val CN = 2
    }
}