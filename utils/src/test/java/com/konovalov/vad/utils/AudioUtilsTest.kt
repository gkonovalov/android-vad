package com.konovalov.vad.utils

import com.konovalov.vad.utils.AudioUtils.getFramesCount
import com.konovalov.vad.utils.AudioUtils.toFloatArray
import com.konovalov.vad.utils.AudioUtils.toShortArray
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
    fun `convert ShortArray to FloatArray`() {
        val input = shortArrayOf(233, 154, 775, 139)
        val expected = floatArrayOf(0.0071108127f, 0.0046998505f, 0.023651846f, 0.004242073f)
        val result = toFloatArray(input)

        assertArrayEquals(expected, result, 0f)
    }

    @Test
    fun `convert ByteArray to FloatArray`() {
        val input = byteArrayOf(11, 12, 23, 35)
        val expected = floatArrayOf(0.09408856f, 0.27414778f)
        val result = toFloatArray(input)

        assertArrayEquals(expected, result, 0f)
    }

    @Test
    fun `convert FloatArray to ShortArray`() {
        val input = floatArrayOf(233f, 154f, 775f, 127f)
        val expected = shortArrayOf(32535, -154, 31992, 32641)
        val result = toShortArray(input)

        assertArrayEquals(expected, result)
    }

    @Test
    fun `convert ByteArray to ShortArray`() {
        val input = byteArrayOf(11, 12, 23, 35)
        val expected = shortArrayOf(3083, 8983)
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