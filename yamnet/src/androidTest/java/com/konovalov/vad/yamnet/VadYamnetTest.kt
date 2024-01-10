package com.konovalov.yamnet

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.konovalov.vad.yamnet.Vad
import com.konovalov.vad.yamnet.VadYamnet
import com.konovalov.vad.yamnet.config.FrameSize
import com.konovalov.vad.yamnet.config.Mode
import com.konovalov.vad.yamnet.config.SampleRate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Georgiy Konovalov on 12/22/2023.
 *
 * Instrumented test for VadYamnet class.
 */
@RunWith(AndroidJUnit4::class)
class VadYamnetTest {

    private lateinit var vad: VadYamnet
    private lateinit var testContext: Context

    @Before
    fun setUp() {
        this.testContext = InstrumentationRegistry.getInstrumentation().context

        this.vad = Vad.builder()
            .setContext(testContext)
            .setSampleRate(SampleRate.SAMPLE_RATE_16K)
            .setFrameSize(FrameSize.FRAME_SIZE_487)
            .setMode(Mode.NORMAL)
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
            false, false, false, false, false, false,
            false, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true
        )

        // Buffer size should be 2x for ByteArray
        val chunkSize = vad.frameSize.value * 2

        // Label for prediction
        val label = "Speech"

        // Reading audio data from test file
        testContext.assets.open("hello.wav").buffered().use { input ->
            // Skip WAV Header
            input.skip(44)

            while (input.available() > 0) {
                // Read audio Frame
                val frameChunk = ByteArray(chunkSize).apply { input.read(this) }

                // Save interference result
                actualResult.add(vad.classifyAudio(label, frameChunk).label == label)
            }
        }

        // Compare expectedResult with actualResult
        assertEquals(expectedResult, actualResult)
    }
}