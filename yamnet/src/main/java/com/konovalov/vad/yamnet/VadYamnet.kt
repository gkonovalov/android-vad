package com.konovalov.vad.yamnet

import android.content.Context
import com.konovalov.vad.utils.AudioUtils.getFramesCount
import com.konovalov.vad.utils.AudioUtils.toShortArray
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
     * Determines if the provided audio data contains speech or other sound.
     * The audio data is passed to the model for prediction. The result is obtained and compared
     * with the threshold value to determine if it represents speech.
     * </p>
     * @param audioData The audio data to analyze.
     * @return List<AudioEvent> list of audio event names.
     */
    fun classifyAudio(audioData: ShortArray): SoundCategory {
        checkState()
        tensor.load(audioData)
        return getResult(classifier.classify(tensor))
    }

    /**
     * <p>
     * Determines whether the given audio frame is speech or not.
     * This method checks if the WEBRTC VAD is initialized and calls the native
     * function to perform the speech detection on the provided audio data.
     * </p>
     * @param audioData: ByteArray - The audio data to analyze.
     * @return {@code true} if the audio data is detected as speech, {@code false} otherwise.
     */
    fun classifyAudio(audioData: ByteArray): SoundCategory {
        return classifyAudio(toShortArray(audioData))
    }

    /**
     * <p>
     * Determines whether the given audio frame is speech or not.
     * This method checks if the WEBRTC VAD is initialized and calls the native
     * function to perform the speech detection on the provided audio data.
     * </p>
     * @param audioData: FloatArray - The audio data to analyze.
     * @return {@code true} if the audio data is detected as speech, {@code false} otherwise.
     */
    fun classifyAudio(audioData: FloatArray): SoundCategory {
        return classifyAudio(toShortArray(audioData))
    }

    /**
     * <p>
     * Continuous Speech listener was designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * </p>
     * @param label: String - expected voice name to be detected.
     * @param audio: ShortArray - The audio data to analyze.
     * @param listener: VadListener - listener to be notified when speech or noise is detected.
     */
    fun setContinuousClassifierListener(label: String, audio: ShortArray, listener: VadListener) {
        continuousClassifierListener(label, classifyAudio(audio), listener)
    }

    /**
     * <p>
     * Continuous Speech listener was designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * Size of audio ByteArray should be 2x of Frame size.
     * </p>
     * @param label: String - expected voice name to be detected.
     * @param audio: ByteArray - The audio data to analyze.
     * @param listener: VadListener - listener to be notified when speech or noise is detected.
     */
    fun setContinuousClassifierListener(label: String, audio: ByteArray, listener: VadListener) {
        continuousClassifierListener(label, classifyAudio(audio), listener)
    }

    /**
     * <p>
     * Continuous Speech listener was designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * </p>
     * @param label: String - expected voice name to be detected.
     * @param audio: FloatArray - The audio data to analyze.
     * @param listener: VadListener - listener to be notified when speech or noise is detected.
     */
    fun setContinuousClassifierListener(label: String, audio: FloatArray, listener: VadListener) {
        continuousClassifierListener(label, classifyAudio(audio), listener)
    }

    /**
     * <p>
     * Continuous Speech listener was designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     * </p>
     * @param label: String - expected sound label to be detected.
     * @param audioEvent: SoundCategory - classified audio category.
     * @param listener: VadListener - listener to be notified when speech or noise is detected.
     */
    private fun continuousClassifierListener(
        label: String,
        audioEvent: SoundCategory,
        listener: VadListener
    ) {
        require(label.isNotEmpty()) {
            "Sound Label required for Continuous Classifier!"
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
            require(supportedParameters[sampleRate]?.contains(frameSize) ?: false) {
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
                "The parameter 'speechDurationMs' cannot be smaller than zero!"
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
     * @param silenceDurationMs The silence duration ms as a Int.
     * @throws IllegalArgumentException if there was negative numbers.
     */
    var silenceDurationMs: Int = silenceDurationMs
        set(silenceDurationMs) {
            require(silenceDurationMs >= 0) {
                "The parameter 'silenceDurationMs' cannot be smaller than zero!"
            }

            field = silenceDurationMs
            maxSilenceFramesCount = getFramesCount(sampleRate.value, frameSize.value, silenceDurationMs)
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
     * Check if VAD session already closed.
     * </p>
     * @throws IllegalArgumentException if session already closed.
     */
    private fun checkState() {
        require(isInitiated) { "You can't use Vad after closing session!" }
    }

    /**
     * <p>
     * Initializes the Tensorflow Lite by creating a classifier with the provided model file.
     * The classifier will be used for making predictions.
     * </p>
     * @param context The context required for accessing the model file.
     */
    init {
        this.sampleRate = sampleRate
        this.frameSize = frameSize
        this.mode = mode
        this.silenceDurationMs = silenceDurationMs
        this.speechDurationMs = speechDurationMs

        this.classifier = AudioClassifier.createFromFileAndOptions(context, "yamnet.tflite",
            AudioClassifier.AudioClassifierOptions.builder()
                .setMaxResults(1)
                .build()
        )
        this.tensor = TensorAudio.create(TensorAudioFormat.builder()
                .setSampleRate(sampleRate.value)
                .build(),
            15600
        )
        this.isInitiated = true
    }
}