package com.konovalov.vad.yamnet

import android.content.Context
import com.konovalov.vad.yamnet.config.FrameSize
import com.konovalov.vad.yamnet.config.Mode
import com.konovalov.vad.yamnet.config.SampleRate
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.audio.TensorAudio.TensorAudioFormat
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications

/**
 * Created by Georgiy Konovalov on 26/06/2023.
 * <p>
 * YAMNet is a deep net that predicts 521 audio event classes(such as Speech, Music, Clapping, etc)
 * from the AudioSet-YouTube corpus it was trained on. It employs the Mobilenet_v1
 * depthwise-separable convolution architecture.
 *
 * The YAMNet VAD supports the following parameters:
 *
 * Sample Rates: 16000Hz
 *
 * Frame Sizes (per sample rate):
 *             For 16000Hz: 243, 487, 731, 975
 *
 * Mode: OFF, NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 * </p>
 * @param context (required) - The context is required for VadYamnet
 * @param sampleRate (required) - The sample rate of the audio input.
 * @param frameSize (required) - The frame size of the audio input.
 * @param mode (required) - The mode of the VAD model.
 * @param speechDurationMs (optional) - used in Continuous Speech detector, the value of this
 * parameter will define the necessary and sufficient duration of negative results
 * to recognize it as silence. Negative numbers are not allowed.
 * @param silenceDurationMs (optional) - used in Continuous Speech detector, the value of
 * this parameter will define the necessary and sufficient duration of positive results to
 * recognize result as speech. Negative numbers are not allowed.
 * </p>
 */
class VadYamnet(
    context: Context,
    sampleRate: SampleRate,
    frameSize: FrameSize,
    mode: Mode,
    speechDurationMs: Int = 0,
    silenceDurationMs: Int = 0
) {

    /**
     * <p>
     * Valid Sample Rates and Frame Sizes for Yamnet VAD DNN model.
     * </p>
     */
    var supportedParameters: Map<SampleRate, Set<FrameSize>> = mapOf(
        SampleRate.SAMPLE_RATE_16K to setOf(
            FrameSize.FRAME_SIZE_243,
            FrameSize.FRAME_SIZE_487,
            FrameSize.FRAME_SIZE_731,
            FrameSize.FRAME_SIZE_975
        )
    )
        private set

    /**
     * Status of VAD initialization.
     */
    private var isInitiated: Boolean = false

    private var tensor: TensorAudio
    private var classifier: AudioClassifier

    private var speechFramesCount = 0
    private var silenceFramesCount = 0
    private var maxSpeechFramesCount = 0
    private var maxSilenceFramesCount = 0

    /**
     * <p>
     * Initializes the Tensorflow Lite by creating a classifier with the provided model file.
     * The classifier will be used for making predictions.
     * </p>
     * @param context The context required for accessing the model file.
     */
    init {
        classifier = AudioClassifier.createFromFileAndOptions(
            context, "yamnet.tflite",
            AudioClassifier.AudioClassifierOptions.builder()
                .setMaxResults(1)
                .build()
        )
        tensor = TensorAudio.create(
            TensorAudioFormat.builder()
                .setSampleRate(sampleRate.value)
                .build(),
            15600
        )
        isInitiated = true
    }

    /**
     * <p>
     * Determines if the provided audio data contains speech or other sound.
     * The audio data is passed to the model for prediction. The result is obtained and compared
     * with the threshold value to determine if it represents speech.
     * </p>
     * @param audioData The audio data to analyze.
     * @return List<AudioEvent> list of audio event names.
     */
    fun classifyAudio(audioData: ShortArray): SoundCategory {
        checkState()

        tensor.load(audioData.map { it / 32767.0f }.toFloatArray())

        return getResult(classifier.classify(tensor))
    }

    /**
     * <p>
     * Continuous Speech listener was designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * </p>
     * @param audio The audio data as a ShortArray.
     * @param listener The listener to be notified when speech or noise is detected.
     */
    fun setContinuousClassifierListener(audio: ShortArray, listener: VadListener) {
        setContinuousClassifierListener("", audio, listener)
    }

    fun setContinuousClassifierListener(label: String, audio: ShortArray, listener: VadListener) {
        val audioEvent = classifyAudio(audio)

        if (label.isEmpty()) {
            listener.onResult(audioEvent)
            return
        }

        if (audioEvent.label.equals(label, true)) {
            silenceFramesCount = 0
            if (++speechFramesCount > maxSpeechFramesCount) {
                speechFramesCount = 0
                listener.onResult(audioEvent)
            }
        } else {
            speechFramesCount = 0
            if (++silenceFramesCount > maxSilenceFramesCount) {
                silenceFramesCount = 0
                listener.onResult(SoundCategory())
            }
        }
    }

    /**
     * <p>
     * The result is obtained and compared with the threshold value to determine if it
     * represents required sound.
     * </p>
     * @param output The result of the inference.
     * @return sound category with label and confidence score.
     */
    private fun getResult(output: List<Classifications>): SoundCategory {
        val label = output.first().categories.filter {
            it.score > threshold()
        }

        if (label.isEmpty()) {
            return SoundCategory()
        }

        return SoundCategory(label.first().label, label.first().score)
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
        Mode.NORMAL -> 0.3f
        Mode.AGGRESSIVE -> 0.5f
        Mode.VERY_AGGRESSIVE -> 0.85f
        else -> 0f
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
            require(
                supportedParameters.containsKey(sampleRate) &&
                        supportedParameters.get(sampleRate)?.contains(frameSize)!!
            ) {
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
     * Closes the Tensorflow Classifier and releases all associated resources.
     * This method should be called when the VAD is no longer needed to free up system resources.
     * </p>
     */
    fun close() {
        checkState()
        isInitiated = false
        classifier.close()
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
        require(isInitiated) { "You can't use Vad after closing session!" }
    }
}