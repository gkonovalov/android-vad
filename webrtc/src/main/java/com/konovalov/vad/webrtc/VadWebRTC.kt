package com.konovalov.vad.webrtc

import com.konovalov.vad.utils.AudioUtils.getFramesCount
import com.konovalov.vad.utils.AudioUtils.toShortArray
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import java.io.Closeable

/**
 * Created by Georgiy Konovalov on 6/1/2023.
 *
 * The WebRTC VAD algorithm, based on GMM, analyzes the audio signal to determine whether it
 * contains speech or non-speech segments.
 *
 * The WebRTC VAD supports the following parameters:
 *
 * Sample Rates:
 *
 *      8000Hz,
 *      16000Hz,
 *      32000Hz,
 *      48000Hz
 *
 * Frame Sizes (per sample rate):
 *
 *    For 8000Hz: 80, 160, 240
 *    For 16000Hz: 160, 320, 480
 *    For 32000Hz: 320, 640, 960
 *    For 48000Hz: 480, 960, 1440
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
 * @param sampleRate        is required for processing audio input.
 * @param frameSize         is required for processing audio input.
 * @param mode              is required for the VAD model.
 * @param speechDurationMs  is minimum duration in milliseconds for speech segments (optional).
 * @param silenceDurationMs is minimum duration in milliseconds for silence segments (optional).
 */
class VadWebRTC(
    sampleRate: SampleRate,
    frameSize: FrameSize,
    mode: Mode,
    speechDurationMs: Int = 0,
    silenceDurationMs: Int = 0
) : Closeable {

    /**
     * Valid sample rates and frame sizes for WebRTC VAD GMM model.
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
     * Determines if the provided audio data contains speech.
     * The audio data is passed to the model for prediction.
     *
     * @param audioData audio data to analyze.
     * @return 'true' if speech is detected, 'false' otherwise.
     */
    fun isSpeech(audioData: ShortArray): Boolean {
        return isContinuousSpeech(predict(audioData))
    }

    /**
     * Determines if the provided audio data contains speech.
     * The audio data is passed to the model for prediction.
     * Size of audio chunk for ByteArray should be 2x of Frame size.
     *
     * @param audioData audio data to analyze.
     * @return 'true' if speech is detected, 'false' otherwise.
     */
    fun isSpeech(audioData: ByteArray): Boolean {
        return isContinuousSpeech(predict(toShortArray(audioData)))
    }

    /**
     * Determines if the provided audio data contains speech.
     * The audio data is passed to the model for prediction.
     *
     * @param audioData audio data to analyze.
     * @return 'true' if speech is detected, 'false' otherwise.
     */
    fun isSpeech(audioData: FloatArray): Boolean {
        return isContinuousSpeech(predict(toShortArray(audioData)))
    }

    /**
     * This method designed to detect long utterances without returning false
     * positive results when user makes pauses between sentences.
     *
     * @param isSpeech predicted frame result.
     * @return 'true' if speech is detected, 'false' otherwise.
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
     * Determines whether the given audio frame is speech or not.
     * This method checks if the WEBRTC VAD is initialized and calls the native
     * function to perform the speech detection on the provided audio data.
     *
     * @param audioData  data to analyze.
     * @return 'true' if the audio data is detected as speech, 'false' otherwise.
     */
    private fun predict(audioData: ShortArray): Boolean {
        checkState()
        return nativeIsSpeech(nativeHandle, sampleRate.value, frameSize.value, audioData)
    }

    /**
     * Set, retrieve and validate sample rate for Vad Model.
     *
     * Valid Sample Rates:
     *
     *      8000Hz,
     *      16000Hz,
     *      32000Hz,
     *      48000Hz
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
     *      For 8000Hz: 80, 160, 240
     *      For 16000Hz: 160, 320, 480
     *      For 32000Hz: 320, 640, 960
     *      For 48000Hz: 480, 960, 1440
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

            checkState()
            nativeSetMode(nativeHandle, mode.value)
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
                "The parameter 'speechDurationMs' should be 0ms >= speechDurationMs <= 300000ms!"
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
                "The parameter 'silenceDurationMs' should be 0ms >= silenceDurationMs <= 300000ms!"
            }

            field = silenceDurationMs
            maxSilenceFramesCount = getFramesCount(sampleRate.value, frameSize.value, silenceDurationMs)
        }

    /**
     * Closes the WebRTC VAD and releases all associated resources.
     * This method should be called when the VAD is no longer needed to free up system resources.
     *
     * @throws IllegalArgumentException if VAD already destroyed.
     */
    override fun close() {
        checkState()
        isInitiated = false
        nativeDestroy(nativeHandle)
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
     * The JNI methods bind to the native WebRTC VAD library.
     */
    private external fun nativeInit(): Long
    private external fun nativeSetMode(nativeHandle: Long, mode: Int): Boolean
    private external fun nativeIsSpeech(nativeHandle: Long, sampleRate: Int, frameSize: Int, audio: ShortArray?): Boolean
    private external fun nativeDestroy(nativeHandle: Long)

    /**
     * Loading the native library required for JNI operations. It uses the System.loadLibrary()
     * method to load the "vad_jni" library during class initialization.
     */
    private companion object {
        init {
            System.loadLibrary("vad_jni")
        }
    }

    /**
     * This constructor Initializes the native component of the WebRTC VAD
     * by calling the native initialization function.
     *
     * @throws IllegalArgumentException if there was an error during initialization of VAD.
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