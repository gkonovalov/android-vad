package com.konovalov.vad.silero

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.content.Context
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
import com.konovalov.vad.utils.AudioUtils.getFramesCount
import com.konovalov.vad.utils.AudioUtils.toFloatArray
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.reflect.safeCast

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
     * Determines if the provided audio data contains speech based on the inference result.
     * The audio data is passed to the model for prediction. The result is obtained and compared
     * with the threshold value to determine if it represents speech.
     * </p>
     * @param audioData: ShortArray - The audio data to analyze.
     * @return True if speech is detected, False otherwise.
     */
    fun isSpeech(audioData: ShortArray): Boolean {
        return isContinuousSpeech(predict(toFloatArray(audioData)))
    }

    /**
     * <p>
     * Determines if the provided audio data contains speech based on the inference result.
     * The audio data is passed to the model for prediction. The result is obtained and compared
     * with the threshold value to determine if it represents speech.
     * Size of audio ByteArray should be 2x of Frame size.
     * </p>
     * @param audioData: ByteArray - The audio data to analyze.
     * @return True if speech is detected, False otherwise.
     */
    fun isSpeech(audioData: ByteArray): Boolean {
        return isContinuousSpeech(predict(toFloatArray(audioData)))
    }

    /**
     * <p>
     * Determines if the provided audio data contains speech based on the inference result.
     * The audio data is passed to the model for prediction. The result is obtained and compared
     * with the threshold value to determine if it represents speech.
     * </p>
     * @param audioData: FloatArray - The audio data to analyze.
     * @return True if speech is detected, False otherwise.
     */
    fun isSpeech(audioData: FloatArray): Boolean {
        return isContinuousSpeech(predict(audioData))
    }

    /**
     * <p>
     * Continuous Speech listener was designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * </p>
     * @param audio: ShortArray - The audio data to analyze.
     * @param listener: VadListener - Listener to be notified when speech or noise is detected.
     * @deprecated This method is deprecated and may be removed in future releases.
     * Please use the 'isSpeech' method for speech analysis instead.
     */
    @Deprecated(
        "Please use the 'isSpeech()' method for speech analysis instead.",
        replaceWith = ReplaceWith(
            "vad.isSpeech(audio)",
            imports = ["com.konovalov.vad.silero.VadSilero"]
        )
    )
    fun setContinuousSpeechListener(audio: ShortArray, listener: VadListener) {
        if (isSpeech(audio)) {
            listener.onSpeechDetected()
        } else {
            listener.onNoiseDetected()
        }
    }

    /**
     * <p>
     * Continuous Speech listener was designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * Size of audio ByteArray should be 2x of Frame size.
     * </p>
     * @param audio: ByteArray - The audio data to analyze.
     * @param listener: VadListener - Listener to be notified when speech or noise is detected.
     * @deprecated This method is deprecated and may be removed in future releases.
     * Please use the 'isSpeech' method for speech analysis instead.
     */
    @Deprecated(
        "Please use the 'isSpeech()' method for speech analysis instead.",
        replaceWith = ReplaceWith(
            "vad.isSpeech(audio)",
            imports = ["com.konovalov.vad.silero.VadSilero"]
        )
    )
    fun setContinuousSpeechListener(audio: ByteArray, listener: VadListener) {
        if (isSpeech(audio)) {
            listener.onSpeechDetected()
        } else {
            listener.onNoiseDetected()
        }
    }

    /**
     * <p>
     * Continuous Speech listener was designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * </p>
     * @param audio: FloatArray - The audio data to analyze.
     * @param listener: VadListener - Listener to be notified when speech or noise is detected.
     * @deprecated This method is deprecated and may be removed in future releases.
     * Please use the 'isSpeech' method for speech analysis instead.
     */
    @Deprecated(
        "Please use the 'isSpeech()' method for speech analysis instead.",
        replaceWith = ReplaceWith(
            "vad.isSpeech(audio)",
            imports = ["com.konovalov.vad.silero.VadSilero"]
        )
    )
    fun setContinuousSpeechListener(audio: FloatArray, listener: VadListener) {
        if (isSpeech(audio)) {
            listener.onSpeechDetected()
        } else {
            listener.onNoiseDetected()
        }
    }

    /**
     * <p>
     * This method designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * </p>
     * @param isSpeech: Boolean - Predicted frame result.
     * @return True if speech is detected, False otherwise.
     */
    private fun isContinuousSpeech(isSpeech: Boolean): Boolean {
        if (isSpeech) {
            if (speechFramesCount <= maxSpeechFramesCount) speechFramesCount++

            if (speechFramesCount > maxSpeechFramesCount) {
                silenceFramesCount = 0
                return true
            }
        } else {
            if (silenceFramesCount <= maxSilenceFramesCount) silenceFramesCount++

            if (silenceFramesCount > maxSilenceFramesCount) {
                speechFramesCount = 0
                return false
            } else if (speechFramesCount > maxSpeechFramesCount) {
                return true
            }
        }
        return false
    }

    /**
     * <p>
     * Determines if the provided audio data contains speech based on the inference result.
     * The audio data is passed to the model for prediction. The result is obtained and compared
     * with the threshold value to determine if it represents speech.
     * </p>
     * @param audioData: FloatArray - The audio data to analyze.
     * @return True if speech is detected, False otherwise.
     */
    private fun predict(audioData: FloatArray): Boolean {
        checkState()
        return getResult(session.run(getInputTensors(audioData))) > threshold()
    }

    /**
     * <p>
     * Retrieves and processes the output tensors to obtain the confidence value.
     * The output tensor contains the confidence value, as well as the updated hidden state (H)
     * and cell state (C) values. The H and C values are flattened and converted to float arrays
     * for further processing.
     * </p>
     * @param output - The result of the inference session.
     * @return The confidence value.
     */
    private fun getResult(output: OrtSession.Result): Float {
        val confidence: Array<FloatArray>? = unpack(output, OutputTensors.OUTPUT)

        flattenArray(unpack(output, OutputTensors.CN))?.let { c = it }
        flattenArray(unpack(output, OutputTensors.HN))?.let { h = it }

        return confidence?.getOrNull(0)?.getOrNull(0) ?: 0f
    }

    /**
     * Unpacks the value of the specified tensor from an OrtSession.Result object
     * and attempts to cast it to an array of the specified generic type {@code T}.
     *
     * @param output The OrtSession.Result object from which to retrieve the value.
     * @param index  The index specifying the position from which to retrieve the value.
     * @param <T>    The generic type to which the value should be cast.
     * @return An array of type {@code T} if the casting is successful,
     *         or {@code null} if an exception occurs
     *         or if the value cannot be cast to the specified type.
     */
    private inline fun <reified T> unpack(output: OrtSession.Result, index: Int): Array<T>? {
        return try {
            Array<T>::class.safeCast(output.get(index).value)
        } catch (e: OrtException) {
            null
        }
    }

    /**
     * Flattens a multi-dimensional array of FloatArrays into a one-dimensional FloatArray.
     *
     * @param array The multi-dimensional array to be flattened.
     * @return A flattened one-dimensional FloatArray if the input array is not null,
     *         or {@code null} if the input array is null.
     */
    private fun flattenArray(array: Array<Array<FloatArray>>?): FloatArray? {
        return array?.flatten()?.flatMap { it.asIterable() }?.toFloatArray()
    }

    /**
     * <p>
     * Creates and returns a map of input tensors for the given audio data, Sample Rate and Frame Size.
     * The audio data is converted to a float array and wrapped in an OnnxTensor with the
     * corresponding tensor shape. The sample rate, hidden state (H), and cell state (C) tensors
     * are also created and added to the map.
     * </p>
     * @param audioData - The audio data as a ShortArray.
     * @throws OrtException if there was an error in creating the tensors or getting the OrtEnvironment.
     * @return A map of input tensors as a Map<String, OnnxTensor>.
     */
    private fun getInputTensors(audioData: FloatArray): Map<String, OnnxTensor> {
        val env = OrtEnvironment.getEnvironment()

        return mapOf(
            InputTensors.INPUT to OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(audioData),
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
     * @param context - The android context.
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
     * Set, retrieve and validate sample rate for Vad Model.
     * </p>
     * @param sampleRate - The sample rate as a SampleRate.
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
     * @param frameSize - The frame size as a FrameSize.
     * @throws IllegalArgumentException if there was invalid frame size.
     */
    var frameSize: FrameSize = frameSize
        set(frameSize) {
            require(supportedParameters[sampleRate]?.contains(frameSize) ?: false) {
                "VAD doesn't support Sample rate:${sampleRate} and Frame Size:${frameSize}!"
            }
            field = frameSize
        }

    /**
     * <p>
     * Set and retrieve mode for Vad Model.
     * </p>
     * @param mode - The mode as a Mode.
     */
    var mode: Mode = mode
        set(mode) {
            field = mode
        }

    /**
     * <p>
     * Set, retrieve and validate speechDurationMs for Vad Model.
     * The value of this parameter will define the necessary and sufficient duration of positive
     * results to recognize result as speech. Negative numbers are not allowed.
     * </p>
     * @param speechDurationMs - The speech duration ms as a Int.
     * @throws IllegalArgumentException if there was negative numbers.
     */
    var speechDurationMs: Int = speechDurationMs
        set(speechDurationMs) {
            require(speechDurationMs in 0..300000) {
                "The parameter 'speechDurationMs' should be >= 0ms and <= 300000ms!"
            }

            field = speechDurationMs
            maxSpeechFramesCount = getFramesCount(sampleRate.value, frameSize.value, speechDurationMs)
        }

    /**
     * <p>
     * Set, retrieve and validate silenceDurationMs for Vad Model.
     * The value of this parameter will define the necessary and sufficient duration of
     * negative results to recognize it as silence. Negative numbers are not allowed.
     * </p>
     * @param silenceDurationMs - The silence duration ms as a Int.
     * @throws IllegalArgumentException if there was negative numbers.
     */
    var silenceDurationMs: Int = silenceDurationMs
        set(silenceDurationMs) {
            require(silenceDurationMs in 0..300000) {
                "The parameter 'silenceDurationMs' should be >= 0ms and <= 300000ms!"
            }

            field = silenceDurationMs
            maxSilenceFramesCount = getFramesCount(sampleRate.value, frameSize.value, silenceDurationMs)
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

    /**
     * <p>
     * Initializes the ONNIX Runtime by creating a session with the provided
     * model file and session options.
     * </p>
     * @param context - The context required for accessing the model file.
     */
    init {
        this.sampleRate = sampleRate
        this.frameSize = frameSize
        this.mode = mode
        this.silenceDurationMs = silenceDurationMs
        this.speechDurationMs = speechDurationMs

        val env = OrtEnvironment.getEnvironment()
        val sessionOptions = SessionOptions()
        sessionOptions.setIntraOpNumThreads(1)
        sessionOptions.setInterOpNumThreads(1)
        sessionOptions.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT)

        this.session = env.createSession(getModel(context), sessionOptions)
        this.isInitiated = true
    }
}