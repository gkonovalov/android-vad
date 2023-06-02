package com.konovalov.vad.example.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.SampleRate

class VoiceRecorder(val callback: AudioCallback) {

    private val TAG = VoiceRecorder::class.java.simpleName

    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private var isListening = false

    private lateinit var sampleRate: SampleRate
    private lateinit var frameSize: FrameSize

    fun start(sampleRate: SampleRate, frameSize: FrameSize) {
        this.sampleRate = sampleRate
        this.frameSize = frameSize
        stop()

        audioRecord = createAudioRecord()
        if (audioRecord != null) {
            isListening = true
            audioRecord?.startRecording()

            thread = Thread(ProcessVoice())
            thread?.start()
        }
    }

    fun stop() {
        isListening = false
        thread?.interrupt()
        thread = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): AudioRecord? {
        try {
            val minBufferSize = maxOf(
                AudioRecord.getMinBufferSize(
                    sampleRate.value,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ),
                2 * frameSize.value
            )

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate.value,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )

            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                return audioRecord
            } else {
                audioRecord.release()
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error can't create AudioRecord ", e)
        }
        return null
    }

    private inner class ProcessVoice : Runnable {
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val size = frameSize.value

            while (!Thread.interrupted() && isListening) {
                val buffer = ShortArray(size)
                audioRecord?.read(buffer, 0, buffer.size)
                callback.onAudio(buffer)
            }
        }
    }

    interface AudioCallback {
        fun onAudio(audioData: ShortArray)
    }
}