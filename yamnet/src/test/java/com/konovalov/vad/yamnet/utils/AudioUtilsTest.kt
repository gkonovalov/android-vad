package com.konovalov.vad.yamnet.utils

import com.konovalov.vad.yamnet.utils.AudioUtils.getFramesCount
import com.konovalov.vad.yamnet.utils.AudioUtils.toShortArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by Georgiy Konovalov on 12/04/2023.
 *
 * Unit tests for AudioUtils class.
 */
class AudioUtilsTest {

    @Test
    fun `convert FloatArray to ShortArray`() {
        val input = floatArrayOf(100F, 200F, 300F)
        val expected = shortArrayOf(-100, -200, -300)
        val result = toShortArray(input)

        assertArrayEquals(expected, result)
    }

    @Test
    fun `convert ByteArray to ShortArray`() {
        val input = byteArrayOf(10, 20, 30)
        val expected = shortArrayOf(5130)
        val result = toShortArray(input)

        assertArrayEquals(expected, result)
    }

    @Test
    fun `getFramesCount calculates frame count correctly`() {
        val sampleRate = 8000
        val frameSize = 256
        val durationMs = 300
        val expected = 9
        val result = getFramesCount(sampleRate, frameSize, durationMs)

        assertEquals(expected, result)
    }
}