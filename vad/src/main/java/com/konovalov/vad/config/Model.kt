package com.konovalov.vad.config

/**
 * Created by Georgiy Konovalov on 1/06/2023.
 * <p>
 * Enum class representing supporting VAD models.
 * </p>
 * @property value The numeric value associated with the Model.
 */
enum class Model(val value: Int) {
    WEB_RTC_GMM(0),
    SILERO_DNN(1);
}