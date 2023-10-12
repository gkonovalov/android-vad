package com.konovalov.vad.utils

/**
 * Created by Georgiy Konovalov on 18/07/2023.
 * <p>
 * AudioUtils is a utility class that provides various functions to work with audio data
 * in different formats. It contains static methods that facilitate audio data conversions,
 * processing, and manipulation. This class aims to simplify common audio-related tasks
 * and ensure reusability of audio processing logic across the application.
 * </p>
 */
object AudioUtils {

    /**
     * <p>
     * Convert audio data from ShortArray to FloatArray.
     * </p>
     * @param audio: ShortArray - audio data for conversion.
     * @return Converted audio data as FloatArray.
     */
    fun toFloatArray(audio: ShortArray): FloatArray {
        return FloatArray(audio.size) { i ->
            audio[i] / 32767.0f
        }
    }

    /**
     * <p>
     * Convert audio data from ByteArray to FloatArray.
     * </p>
     * @param audio: ByteArray - audio data for conversion.
     * @return Converted audio data as FloatArray.
     */
    fun toFloatArray(audio: ByteArray): FloatArray {
        return FloatArray(audio.size / 2) { i ->
            ((audio[2 * i].toInt() and 0xFF) or (audio[2 * i + 1].toInt() shl 8)) / 32767.0f
        }
    }

    /**
     * <p>
     * Convert audio data from FloatArray to ShortArray.
     * </p>
     * @param audio: FloatArray - audio data for conversion.
     * @return Converted audio data as ShortArray.
     */
    fun toShortArray(audio: FloatArray): ShortArray {
        return ShortArray(audio.size) { i ->
            (audio[i] * 32767.0f).toInt().toShort()
        }
    }

    /**
     * <p>
     * Convert audio data from ByteArray to ShortArray.
     * </p>
     * @param audio: ByteArray - audio data for conversion.
     * @return Converted audio data as ShortArray.
     */
    fun toShortArray(audio: ByteArray): ShortArray {
        return ShortArray(audio.size / 2) { i ->
            ((audio[2 * i].toInt() and 0xFF) or (audio[2 * i + 1].toInt() shl 8)).toShort()
        }
    }

    /**
     * <p>
     * Calculates the frame count based on the duration in milliseconds,
     * frequency and frame size.
     * </p>
     * @param durationMs The duration in milliseconds.
     * @return The frame count.
     */
    fun getFramesCount(sampleRate: Int, frameSize: Int, durationMs: Int): Int {
        return durationMs / (frameSize / (sampleRate / 1000))
    }
}