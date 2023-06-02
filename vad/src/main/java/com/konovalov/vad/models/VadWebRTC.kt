package com.konovalov.vad.models

import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate

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
 *
 * Mode: NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
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
 */
internal class VadWebRTC(
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

    /**
     * Native handle used for interacting with the underlying native WebRTC VAD implementation.
     */
    private var nativeHandle: Long = -1

    /**
     * <p>
     * This constructor initializes the WebRTC VAD and sets the voice filtration mode.
     * </p>
     * @throws IllegalArgumentException If there was an error initializing the VAD.
     */
    init {
        nativeHandle = nativeInit()
        require(isVadInitialized()) { "Error can't init WebRTC VAD!!" }
        setMode()
    }

    /**
     * <p>
     * Determines whether the given audio data is speech or not.
     * This method checks if the WEBRTC VAD is initialized and calls the native
     * function to perform the speech detection on the provided audio data.
     * </p>
     * @param audioData The audio data to analyze.
     * @return {@code true} if the audio data is detected as speech, {@code false} otherwise.
     */
    override fun isSpeech(audioData: ShortArray): Boolean {
        if (isVadInitialized()) {
            return nativeIsSpeech(nativeHandle, sampleRate.value, frameSize.value, audioData)
        }
        return false
    }

    /**
     * Sets the mode of the WebRTC VAD.
     * <p>The mode determines the aggressiveness of the voice activity detection algorithm.</p>
     * @param mode The mode to set.
     */
    override var mode: Mode
        get() = super.mode
        set(mode) {
            super.mode = mode
            setMode()
        }

    /**
     * <p>
     * Return current model type
     * </p>
     * @return {@code Model.WEB_RTC_GMM}
     */
    override val model: Model
        get() = Model.WEB_RTC_GMM

    /**
     * <p>
     * Closes the WebRTC VAD and releases any associated resources.
     * This method should be called when the VAD is no longer needed to free up system resources.
     * </p>
     */
    override fun close() {
        if (isVadInitialized()) {
            nativeDestroy(nativeHandle)
            nativeHandle = -1
        }
    }

    private fun setMode() {
        if (isVadInitialized()) {
            nativeSetMode(nativeHandle, mode.value)
        }
    }

    /**
     * <p>
     * Checks if the WebRTC VAD is initialized.
     * </p>
     * @return true if the VAD is initialized, false otherwise.
     */
    private fun isVadInitialized(): Boolean = nativeHandle > 0

    private external fun nativeInit(): Long
    private external fun nativeSetMode(nativeHandle: Long, mode: Int): Boolean
    private external fun nativeIsSpeech(nativeHandle: Long, sampleRate: Int, frameSize: Int, audio: ShortArray?): Boolean
    private external fun nativeDestroy(nativeHandle: Long)

    /**
     * <p>Loading the native library required for JNI operations. It uses the System.loadLibrary()
     * method to load the "vad_jni" library during class initialization.
     */
    companion object {
        init {
            System.loadLibrary("vad_jni")
        }
    }
}