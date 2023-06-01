package com.konovalov.vad

interface VadListener {
    fun onSpeechDetected()
    fun onNoiseDetected()
}