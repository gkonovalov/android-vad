package com.konovalov.vad.models

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.content.Context
import com.konovalov.vad.R
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.reflect.cast

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * The Silero VAD algorithm, based on DNN, analyzes the audio signal to determine whether it
 * contains speech or non-speech segments. It offers higher accuracy in differentiating speech from
 * background noise compared to the WebRTC VAD algorithm.
 *
 * The Silero VAD supports the following parameters:
 *
 * Sample Rates: 8000Hz, 16000Hz
 *
 * Frame Sizes (per sample rate):
 *             For 8000Hz: 256, 512, 768
 *             For 16000Hz: 512, 1024, 1536
 *
 * Mode: NORMAL, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * Please note that the VAD class supports these specific combinations of sample
 * rates and frame sizes, and the classifiers determine the aggressiveness of the voice
 * activity detection algorithm.
 * </p>
 * @param context The context is required and helps with reading the model file from the file system.
 * @param sampleRate The sample rate of the audio input.
 * @param frameSize The frame size of the audio input.
 * @param mode The mode of the VAD model.
 * @param speechDurationMs The minimum duration in milliseconds for speech segments.
 * @param silenceDurationMs The minimum duration in milliseconds for silence segments.
 */
internal class VadSilero(
    context: Context,
    sampleRate: SampleRate,
    frameSize: FrameSize,
    mode: Mode,
    speechDurationMs: Int,
    silenceDurationMs: Int
) : VadModel(
    sampleRate,
    frameSize,
    mode,
    speechDurationMs,
    silenceDurationMs
) {

    private var session: OrtSession? = null

    private val THREADS_COUNT = 1

    private var h = FloatArray(128)
    private var c = FloatArray(128)

    init {
        val env = OrtEnvironment.getEnvironment()
        val sessionOptions = SessionOptions()
        sessionOptions.setIntraOpNumThreads(THREADS_COUNT)
        sessionOptions.setInterOpNumThreads(THREADS_COUNT)
        sessionOptions.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT)
        session = env.createSession(getModel(context), sessionOptions)
    }

    override fun isSpeech(audioData: ShortArray): Boolean {
        return getResult(session?.run(getInputTensors(audioData))) > threshold()
    }

    private fun getResult(output: OrtSession.Result?): Float {
        val confidence = Array<FloatArray>::class.cast(output?.get(OutputTensors.OUTPUT)?.value)
        val hn = Array<Array<FloatArray>>::class.cast(output?.get(OutputTensors.HN)?.value)
        val cn = Array<Array<FloatArray>>::class.cast(output?.get(OutputTensors.CN)?.value)

        h = hn.flatten().flatMap { it.asIterable() }.toFloatArray()
        c = cn.flatten().flatMap { it.asIterable() }.toFloatArray()

        return confidence[0][0]
    }

    private fun getInputTensors(audioData: ShortArray): Map<String, OnnxTensor> {
        val env = OrtEnvironment.getEnvironment()

        return mapOf(
            InputTensors.INPUT to OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(audioData.map { it / 32768.0f }.toFloatArray()),
                longArrayOf(1, frameSize.value.toLong())
            ),
            InputTensors.SR to OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(longArrayOf(sampleRate.value.toLong())),
                longArrayOf(1)
            ),
            InputTensors.H to OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(h),
                longArrayOf(2, 1, 64)
            ),
            InputTensors.C to OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(c),
                longArrayOf(2, 1, 64)
            )
        )
    }

    private fun getModel(context: Context): ByteArray {
        return context.resources.openRawResource(R.raw.silero_vad).readBytes()
    }

    private fun threshold(): Float = when (mode) {
        Mode.AGGRESSIVE -> 0.8f
        Mode.VERY_AGGRESSIVE -> 0.95f
        else -> 0.5f
    }

    override val model: Model
        get() = Model.SILERO_DNN

    override fun close() {
        session?.close()
        session = null
    }

    object InputTensors {
        const val INPUT = "input"
        const val SR = "sr"
        const val H = "h"
        const val C = "c"
    }

    object OutputTensors {
        const val OUTPUT = 0
        const val HN = 1
        const val CN = 2
    }
}