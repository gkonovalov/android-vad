package com.konovalov.vad.yamnet

/**
 * Created by Georgiy Konovalov on 6/1/2023.
 *
 * Represents a sound category with a label and a corresponding score.
 * This data class is typically used for classifying different types of sounds.
 */
data class SoundCategory(var label: String = "Silence", var score: Float = 0F)
