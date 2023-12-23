package com.konovalov.webrtc

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.konovalov.vad.webrtc.Vad
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Georgiy Konovalov on 12/22/2023.
 * <p>
 * Instrumented test for VadWebRTC class.
 * </p>
 */
@RunWith(AndroidJUnit4::class)
class VadWebRTCTest {

    private lateinit var vad: VadWebRTC
    private lateinit var testContext: Context

    @Before
    fun setUp() {
        this.testContext = InstrumentationRegistry.getInstrumentation().context

        this.vad = Vad.builder()
            .setSampleRate(SampleRate.SAMPLE_RATE_16K)
            .setFrameSize(FrameSize.FRAME_SIZE_320)
            .setMode(Mode.VERY_AGGRESSIVE)
            .build()
    }

    @After
    fun shutdown() {
        vad.close()
    }

    @Test
    fun testIsSpeech() {
        // List for VAD results
        val actualResult = mutableListOf<Boolean>()

        // List with expected VAD results
        val expectedResult = listOf(
            false, false, false, true, true, true, true, false,
            false, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true
        )

        // Buffer size should be 2x for ByteArray
        val chunkSize = vad.frameSize.value * 2

        // Reading audio data from test file
        testContext.assets.open("hello.wav").buffered().use { input ->
            // Skip WAV Header
            input.skip(44)

            while (input.available() > 0) {
                // Read audio Frame
                val frameChunk = ByteArray(chunkSize).apply { input.read(this) }

                // Save interference result
                actualResult.add(vad.isSpeech(frameChunk))
            }
        }

        // Compare expectedResult with actualResult
        assertEquals(expectedResult, actualResult)
    }
}