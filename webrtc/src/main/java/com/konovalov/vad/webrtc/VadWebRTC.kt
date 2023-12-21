package com.konovalov.vad.webrtc

import com.konovalov.vad.utils.AudioUtils.getFramesCount
import com.konovalov.vad.utils.AudioUtils.toShortArray
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import java.io.Closeable

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * The WebRTC VAD algorithm, based on GMM, analyzes the audio signal to determine whether it
 * contains speech or non-speech segments.
 *
 * The WebRTC VAD supports the following parameters:
 *
 * Sample Rates: 8000Hz, 16000Hz, 32000Hz, 48000Hz
 *
 * Frame Sizes (per sample rate):
 *             For 8000Hz: 80, 160, 240
 *             For 16000Hz: 160, 320, 480
 *             For 32000Hz: 320, 640, 960
 *             For 48000Hz: 480, 960, 1440
 * Mode: NORMAL, LOW_BITRATE, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 * </p>
 * @param sampleRate The sample rate of the audio input.
 * @param frameSize The frame size of the audio input.
 * @param mode The mode of the VAD model.
 * @param speechDurationMs The minimum duration in milliseconds for speech segments.
 * @param silenceDurationMs The minimum duration in milliseconds for silence segments.
 * </p>
 */
class VadWebRTC(
    sampleRate: SampleRate,
    frameSize: FrameSize,
    mode: Mode,
    speechDurationMs: Int = 0,
    silenceDurationMs: Int = 0
) : Closeable {

    /**
     * <p>
     * Valid sample rates and frame sizes for WebRTC VAD GMM model.
     * </p>
     */
    var supportedParameters: Map<SampleRate, Set<FrameSize>> = mapOf(
        SampleRate.SAMPLE_RATE_8K to setOf(
            FrameSize.FRAME_SIZE_80,
            FrameSize.FRAME_SIZE_160,
            FrameSize.FRAME_SIZE_240
        ),
        SampleRate.SAMPLE_RATE_16K to setOf(
            FrameSize.FRAME_SIZE_160,
            FrameSize.FRAME_SIZE_320,
            FrameSize.FRAME_SIZE_480
        ),
        SampleRate.SAMPLE_RATE_32K to setOf(
            FrameSize.FRAME_SIZE_320,
            FrameSize.FRAME_SIZE_640,
            FrameSize.FRAME_SIZE_960
        ),
        SampleRate.SAMPLE_RATE_48K to setOf(
            FrameSize.FRAME_SIZE_480,
            FrameSize.FRAME_SIZE_960,
            FrameSize.FRAME_SIZE_1440
        )
    )
        private set

    /**
     * Status of VAD initialization.
     */
    private var isInitiated: Boolean = false

    /**
     * Native handle used for interacting with the underlying native WebRTC VAD implementation.
     */
    private var nativeHandle: Long = -1

    private var speechFramesCount = 0
    private var silenceFramesCount = 0
    private var maxSpeechFramesCount = 0
    private var maxSilenceFramesCount = 0

    /**
     * <p>
     * Determines if the provided audio data contains speech.
     * The audio data is passed to the model for prediction.
     * </p>
     * @param audioData: ShortArray - The audio data to analyze.
     * @return True if speech is detected, False otherwise.
     */
    fun isSpeech(audioData: ShortArray): Boolean {
        return isContinuousSpeech(predict(audioData))
    }

    /**
     * <p>
     * Determines if the provided audio data contains speech.
     * The audio data is passed to the model for prediction.
     * Size of audio ByteArray should be 2x of Frame size.
     * </p>
     * @param audioData: ByteArray - The audio data to analyze.
     * @return True if speech is detected, False otherwise.
     */
    fun isSpeech(audioData: ByteArray): Boolean {
        return isContinuousSpeech(predict(toShortArray(audioData)))
    }

    /**
     * <p>
     * Determines if the provided audio data contains speech.
     * The audio data is passed to the model for prediction.
     * </p>
     * @param audioData: FloatArray - The audio data to analyze.
     * @return True if speech is detected, False otherwise.
     */
    fun isSpeech(audioData: FloatArray): Boolean {
        return isContinuousSpeech(predict(toShortArray(audioData)))
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
    @Deprecated("Please use the 'isSpeech()' method for speech analysis instead.",
        replaceWith = ReplaceWith(
            "vad.isSpeech(audio)",
            imports = ["com.konovalov.vad.webrtc"]
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
    @Deprecated("Please use the 'isSpeech()' method for speech analysis instead.",
        replaceWith = ReplaceWith(
            "vad.isSpeech(audio)",
            imports = ["com.konovalov.vad.webrtc"]
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
    @Deprecated("Please use the 'isSpeech()' method for speech analysis instead.",
        replaceWith = ReplaceWith(
            "vad.isSpeech(audio)",
            imports = ["com.konovalov.vad.webrtc"]
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
     * Determines whether the given audio frame is speech or not.
     * This method checks if the WEBRTC VAD is initialized and calls the native
     * function to perform the speech detection on the provided audio data.
     * </p>
     * @param audioData: ShortArray - The audio data to analyze.
     * @return {@code true} if the audio data is detected as speech, {@code false} otherwise.
     */
    private fun predict(audioData: ShortArray): Boolean {
        checkState()
        return nativeIsSpeech(nativeHandle, sampleRate.value, frameSize.value, audioData)
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

            checkState()
            nativeSetMode(nativeHandle, mode.value)
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
     * @param silenceDurationMs - The silence duration ms as a Int.
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
     * Closes the WebRTC VAD and releases all associated resources.
     * This method should be called when the VAD is no longer needed to free up system resources.
     * </p>
     * @throws IllegalArgumentException if VAD already destroyed.
     */
    override fun close() {
        checkState()
        isInitiated = false
        nativeDestroy(nativeHandle)
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
     * The JNI methods bind to the native WebRTC VAD library.
     * </p>
     */
    private external fun nativeInit(): Long
    private external fun nativeSetMode(nativeHandle: Long, mode: Int): Boolean
    private external fun nativeIsSpeech(nativeHandle: Long, sampleRate: Int, frameSize: Int, audio: ShortArray?): Boolean
    private external fun nativeDestroy(nativeHandle: Long)

    /**
     * <p>
     * Loading the native library required for JNI operations. It uses the System.loadLibrary()
     * method to load the "vad_jni" library during class initialization.
     * </p>
     */
    private companion object {
        init {
            System.loadLibrary("vad_jni")
        }
    }

    /**
     * <p>
     * This constructor Initializes the native component of the WebRTC VAD
     * by calling the native initialization function.
     * </p>
     * @throws IllegalArgumentException if there was an error initializing the VAD.
     */
    init {
        this.nativeHandle = nativeInit()
        this.isInitiated = nativeHandle != -1L && nativeHandle != 0L

        this.sampleRate = sampleRate
        this.frameSize = frameSize
        this.mode = mode
        this.silenceDurationMs = silenceDurationMs
        this.speechDurationMs = speechDurationMs
    }
}