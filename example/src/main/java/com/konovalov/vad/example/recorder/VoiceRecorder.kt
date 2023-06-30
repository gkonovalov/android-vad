package com.konovalov.vad.example.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log

class VoiceRecorder(val callback: AudioCallback) {

    private val TAG = VoiceRecorder::class.java.simpleName

    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private var isListening = false

    private var sampleRate: Int = 0
    private var frameSize: Int = 0

    fun start(sampleRate: Int, frameSize: Int) {
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
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ),
                2 * frameSize
            )

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
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
            val size = frameSize

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