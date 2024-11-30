package com.konovalov.vad.yamnet

import android.content.Context
import com.konovalov.vad.yamnet.utils.AudioUtils.getFramesCount
import com.konovalov.vad.yamnet.utils.AudioUtils.toShortArray
import com.konovalov.vad.yamnet.config.FrameSize
import com.konovalov.vad.yamnet.config.Mode
import com.konovalov.vad.yamnet.config.SampleRate
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.audio.TensorAudio.TensorAudioFormat
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import java.io.Closeable

/**
 * Created by Georgiy Konovalov on 6/26/2023.
 *
 * YAMNet is a deep net that predicts 521 audio event classes(such as Speech, Music, Clapping, etc)
 * from the AudioSet-YouTube corpus it was trained on. It employs the Mobilenet_v1
 * depthwise-separable convolution architecture.
 *
 * The YAMNet VAD supports the following parameters:
 *
 * Sample Rates:
 *
 *      16000Hz
 *
 * Frame Sizes (per sample rate):
 *
 *      For 16000Hz: 243, 487, 731, 975
 *
 * Mode:
 *
 *    NORMAL,
 *    LOW_BITRATE,
 *    AGGRESSIVE,
 *    VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 *
 * @param context           is required for reading the model file from file system.
 * @param sampleRate        is required for processing audio input.
 * @param frameSize         is required for processing audio input.
 * @param mode              is mode is required for the VAD model.
 * @param speechDurationMs  is minimum duration in milliseconds for speech segments (optional).
 * @param silenceDurationMs is minimum duration in milliseconds for silence segments (optional).
 */
class VadYamnet(
    context: Context,
    sampleRate: SampleRate,
    frameSize: FrameSize,
    mode: Mode,
    speechDurationMs: Int = 0,
    silenceDurationMs: Int = 0
) : Closeable {

    /**
     * Valid Sample Rates and Frame Sizes for Yamnet VAD DNN model.
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
     * Determines if the provided audio data contains specific sound.
     * The audio data is passed to the model for prediction.
     * The result is obtained and compared with the threshold value to determine
     * if it represents specific sound.
     *
     * @param audioData audio data to analyze.
     * @return SoundCategory with label and confidence score.
     */
    fun classifyAudio(audioData: ShortArray): SoundCategory {
        return predict(audioData)
    }

    /**
     * Determines if the provided audio data contains specific sound.
     * The audio data is passed to the model for prediction.
     * The result is obtained and compared with the threshold value to determine
     * if it represents specific sound.
     * Size of audio chunk for ByteArray should be 2x of Frame size.
     *
     * @param audioData audio data to analyze.
     * @return SoundCategory with label and confidence score.
     */
    fun classifyAudio(audioData: ByteArray): SoundCategory {
        return predict(toShortArray(audioData))
    }

    /**
     * Determines if the provided audio data contains specific sound.
     * The audio data is passed to the model for prediction.
     * The result is obtained and compared with the threshold value to determine
     * if it represents specific sound.
     *
     * @param audioData audio data to analyze.
     * @return SoundCategory with label and confidence score.
     */
    fun classifyAudio(audioData: FloatArray): SoundCategory {
        return predict(toShortArray(audioData))
    }

    /**
     * Determines if the provided audio data contains specific sound.
     * The audio data is passed to the model for prediction.
     * The result is obtained and compared with the threshold value to determine
     * if it represents specific sound. This method designed to detect long utterances
     * without returning false positive results when user makes pauses between sentences.
     *
     * @param label     expected sound name to be detected.
     * @param audioData audio data to analyze.
     * @return SoundCategory with label and confidence score.
     */
    fun classifyAudio(label: String, audioData: ShortArray): SoundCategory {
        return isContinuousVoice(label, classifyAudio(audioData))
    }

    /**
     * Determines if the provided audio data contains specific sound.
     * The audio data is passed to the model for prediction.
     * The result is obtained and compared with the threshold value to determine
     * if it represents specific sound. This method designed to detect long utterances
     * without returning false positive results when user makes pauses between sentences.
     * Size of audio chunk for ByteArray should be 2x of Frame size.
     *
     * @param label     expected sound name to be detected.
     * @param audioData audio data to analyze.
     * @return SoundCategory with label and confidence score.
     */
    fun classifyAudio(label: String, audioData: ByteArray): SoundCategory {
        return isContinuousVoice(label, classifyAudio(audioData))
    }

    /**
     * Determines if the provided audio data contains specific sound.
     * The audio data is passed to the model for prediction.
     * The result is obtained and compared with the threshold value to determine
     * if it represents specific sound. This method designed to detect long utterances
     * without returning false positive results when user makes pauses between sentences.
     *
     * @param label     expected sound name to be detected.
     * @param audioData audio data to analyze.
     * @return SoundCategory with label and confidence score.
     */
    fun classifyAudio(label: String, audioData: FloatArray): SoundCategory {
        return isContinuousVoice(label, classifyAudio(audioData))
    }

    /**
     * This method designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     *
     * @param label      expected sound label to be detected.
     * @param audioEvent predicted sound category.
     * @return SoundCategory with label and confidence score.
     */
    private fun isContinuousVoice(label: String, audioEvent: SoundCategory): SoundCategory {
        require(label.isNotEmpty()) {
            "Sound Label required for Continuous classifier!"
        }

        if (audioEvent.label.equals(label, true)) {
            if (speechFramesCount <= maxSpeechFramesCount) speechFramesCount++

            if (speechFramesCount > maxSpeechFramesCount) {
                silenceFramesCount = 0
                return audioEvent
            }
        } else {
            if (silenceFramesCount <= maxSilenceFramesCount) silenceFramesCount++

            if (silenceFramesCount > maxSilenceFramesCount) {
                speechFramesCount = 0
                return SoundCategory()
            } else if (speechFramesCount > maxSpeechFramesCount) {
                return SoundCategory(label)
            }
        }

        return SoundCategory()
    }

    /**
     * Determines if the provided audio data contains speech or other sound.
     * The audio data is passed to the model for prediction. The result is obtained and compared
     * with the threshold value to determine if it represents specific sound.
     *
     * @param audioData audio data to analyze.
     * @return SoundCategory with label and confidence score.
     */
    private fun predict(audioData: ShortArray): SoundCategory {
        checkState()
        tensor.load(audioData)
        return getResult(classifier.classify(tensor))
    }

    /**
     * The result is obtained and compared with the threshold value to determine if it
     * represents required sound.
     *
     * @param output result of the inference as List<Classifications>.
     * @return SoundCategory with label and confidence score.
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
     * Calculates and returns the threshold value based on the value of detection mode.
     * The threshold value represents the confidence level required for VAD to make proper decision.
     * ex. Mode.VERY_AGGRESSIVE requiring a very high prediction accuracy from the model.
     *
     * @return threshold Float value.
     */
    private fun threshold(): Float = when (mode) {
        Mode.NORMAL -> 0.3f
        Mode.AGGRESSIVE -> 0.5f
        Mode.VERY_AGGRESSIVE -> 0.85f
        else -> 0f
    }

    /**
     * Set, retrieve and validate sample rate for Vad Model.
     *
     * Valid Sample Rates:
     *
     *      16000Hz
     *
     * @param sampleRate is required for processing audio input.
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
     * Set, retrieve and validate frame size for Vad Model.
     *
     * Valid Frame Sizes (per sample rate):
     *
     *      For 16000Hz: 243, 487, 731, 975
     *
     * @param frameSize is required for processing audio input.
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
     * Set and retrieve detection mode for Vad model.
     *
     * Mode:
     *
     *    NORMAL,
     *    LOW_BITRATE,
     *    AGGRESSIVE,
     *    VERY_AGGRESSIVE
     *
     * @param mode is required for the VAD model.
     */
    var mode: Mode = mode
        set(mode) {
            field = mode
        }

    /**
     * Set, retrieve and validate speechDurationMs for Vad Model.
     * The value of this parameter will define the necessary and sufficient duration of positive
     * results to recognize result as speech. This parameter is optional.
     *
     * Permitted range (0ms >= speechDurationMs <= 300000ms).
     *
     * Parameters used for {@link VadSilero.isSpeech}.
     *
     * @param speechDurationMs speech duration ms.
     * @throws IllegalArgumentException if out of permitted range.
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
     * Set, retrieve and validate silenceDurationMs for Vad Model.
     * The value of this parameter will define the necessary and sufficient duration of
     * negative results to recognize it as silence. This parameter is optional.
     *
     * Permitted range (0ms >= silenceDurationMs <= 300000ms).
     *
     * Parameters used in {@link VadSilero.isSpeech}.
     *
     * @param silenceDurationMs silence duration ms.
     * @throws IllegalArgumentException if out of permitted range.
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
     * Closes the Tensorflow Classifier and releases all associated resources.
     * This method should be called when the VAD is no longer needed to free up system resources.
     *
     * @throws IllegalArgumentException if session already closed.
     */
    override fun close() {
        checkState()
        isInitiated = false
        classifier.close()
    }

    /**
     * Check if VAD session already closed.
     *
     * @throws IllegalArgumentException if session already closed.
     */
    private fun checkState() {
        require(isInitiated) { "You can't use Vad after closing session!" }
    }

    /**
     * Initializes the Tensorflow Lite by creating a classifier with the provided model file.
     * The classifier will be used for making predictions.
     *
     * @param context is required for accessing the model file.
     * @throws IllegalArgumentException if there was an error during initialization of VAD.
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