package com.konovalov.vad.yamnet

/**
 * Created by Georgiy Konovalov on 26/06/2023.
 *
 * Interface representing a listener for Voice Activity Detection (VAD) events.
 */
interface VadListener {
    fun onResult(event: SoundCategory)
}