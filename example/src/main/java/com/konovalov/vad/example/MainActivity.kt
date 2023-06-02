package com.konovalov.vad.example

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.konovalov.vad.models.VadModel
import com.konovalov.vad.VadListener
import com.konovalov.vad.Validator.getValidFrameSizes
import com.konovalov.vad.Validator.getValidSampleRates
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate
import com.konovalov.vad.example.recorder.VoiceRecorder
import com.konovalov.vad.example.recorder.VoiceRecorder.AudioCallback
import com.konovalov.vad.Vad
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class MainActivity : AppCompatActivity(), AudioCallback, View.OnClickListener,
    AdapterView.OnItemSelectedListener {

    private val DEFAULT_MODEL = Model.WEB_RTC_GMM
    private val DEFAULT_SAMPLE_RATE = SampleRate.SAMPLE_RATE_8K
    private val DEFAULT_FRAME_SIZE = FrameSize.FRAME_SIZE_80
    private val DEFAULT_MODE = Mode.VERY_AGGRESSIVE
    private val DEFAULT_SILENCE_DURATION_MS = 300
    private val DEFAULT_SPEECH_DURATION_MS = 50

    private val SPINNER_MODEL_TAG = "model"
    private val SPINNER_SAMPLE_RATE_TAG = "sample_rate"
    private val SPINNER_FRAME_SIZE_TAG = "frame_size"
    private val SPINNER_MODE_TAG = "mode"

    private lateinit var recordingButton: FloatingActionButton
    private lateinit var speechTextView: TextView

    private lateinit var sampleRateSpinner: Spinner
    private lateinit var frameSpinner: Spinner
    private lateinit var modeSpinner: Spinner
    private lateinit var modelSpinner: Spinner

    private lateinit var sampleRateAdapter: ArrayAdapter<String>
    private lateinit var frameAdapter: ArrayAdapter<String>
    private lateinit var modeAdapter: ArrayAdapter<String>
    private lateinit var modelAdapter: ArrayAdapter<String>

    private lateinit var recorder: VoiceRecorder
    private lateinit var vad: VadModel
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vad = Vad.builder()
            .setModel(DEFAULT_MODEL)
            .setSampleRate(DEFAULT_SAMPLE_RATE)
            .setFrameSize(DEFAULT_FRAME_SIZE)
            .setMode(DEFAULT_MODE)
            .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
            .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
            .setContext(applicationContext)
            .build()

        recorder = VoiceRecorder(this)

        speechTextView = findViewById(R.id.speechTextView)
        sampleRateSpinner = findViewById(R.id.sampleRateSpinner)
        sampleRateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, getSampleRates(DEFAULT_MODEL))
        sampleRateSpinner.adapter = sampleRateAdapter
        sampleRateSpinner.tag = SPINNER_SAMPLE_RATE_TAG
        sampleRateSpinner.setSelection(getSampleRates(DEFAULT_MODEL).indexOf(DEFAULT_SAMPLE_RATE.name), false)
        sampleRateSpinner.onItemSelectedListener = this

        frameSpinner = findViewById(R.id.frameSampleRateSpinner)
        frameAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, getFrameSizes(DEFAULT_MODEL, DEFAULT_SAMPLE_RATE))
        frameSpinner.adapter = frameAdapter
        frameSpinner.tag = SPINNER_FRAME_SIZE_TAG
        frameSpinner.setSelection(getFrameSizes(DEFAULT_MODEL, DEFAULT_SAMPLE_RATE).indexOf(DEFAULT_FRAME_SIZE.name), false)
        frameSpinner.onItemSelectedListener = this

        modeSpinner = findViewById(R.id.modeSpinner)
        modeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes())
        modeSpinner.adapter = modeAdapter
        modeSpinner.tag = SPINNER_MODE_TAG
        modeSpinner.setSelection(modes().indexOf(DEFAULT_MODE.name), false)
        modeSpinner.onItemSelectedListener = this

        modelSpinner = findViewById(R.id.modelSpinner)
        modelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, models())
        modelSpinner.adapter = modelAdapter
        modelSpinner.tag = SPINNER_MODEL_TAG
        modelSpinner.setSelection(modes().indexOf(DEFAULT_MODEL.name), false)
        modelSpinner.onItemSelectedListener = this

        recordingButton = findViewById(R.id.recordingActionButton)
        recordingButton.setOnClickListener(this)
        recordingButton.isEnabled = false

        activateRecordingButtonWithPermissionCheck()
    }

    override fun onAudio(audioData: ShortArray) {
        vad.setContinuousSpeechListener(audioData, object : VadListener {
            override fun onSpeechDetected() {
                runOnUiThread { speechTextView.setText(R.string.speech_detected) }
            }

            override fun onNoiseDetected() {
                runOnUiThread { speechTextView.setText(R.string.noise_detected) }
            }
        })
    }

    private fun getSampleRates(model: Model): List<String> {
        return getValidSampleRates(model).map { it.name }.toList()
    }

    private fun getFrameSizes(model: Model, sampleRate: SampleRate): List<String> {
        return getValidFrameSizes(model, sampleRate).map { it.name }.toList()
    }

    private fun modes(): List<String> {
        return Mode.values().map { it.name }.toList()
    }

    private fun models(): List<String> {
        return Model.values().map { it.name }.toList()
    }

    private fun startRecording() {
        isRecording = true
        recorder.start(vad.sampleRate, vad.frameSize)
        recordingButton.setImageResource(R.drawable.stop)
    }

    private fun stopRecording() {
        isRecording = false
        recorder.stop()
        recordingButton.setImageResource(R.drawable.red_dot)
    }

    override fun onClick(v: View) {
        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View, position: Int, l: Long) {
        stopRecording()

        when (adapterView.tag.toString()) {
            SPINNER_MODEL_TAG -> {
                val model = Model.valueOf(modelAdapter.getItem(position).toString())

                sampleRateAdapter.clear()
                sampleRateAdapter.addAll(getSampleRates(model))
                sampleRateAdapter.notifyDataSetChanged()
                sampleRateSpinner.setSelection(0)

                val sampleRate = SampleRate.valueOf(sampleRateAdapter.getItem(0).toString())

                frameAdapter.clear()
                frameAdapter.addAll(getFrameSizes(model, sampleRate))
                frameAdapter.notifyDataSetChanged()
                frameSpinner.setSelection(0)

                val frameSize = FrameSize.valueOf(frameAdapter.getItem(0).toString())

                modeSpinner.setSelection(modes().indexOf(DEFAULT_MODE.name))

                vad.close()
                vad = Vad.builder()
                    .setModel(model)
                    .setSampleRate(sampleRate)
                    .setFrameSize(frameSize)
                    .setMode(DEFAULT_MODE)
                    .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
                    .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
                    .setContext(this)
                    .build()
            }


            SPINNER_SAMPLE_RATE_TAG -> {
                vad.sampleRate = SampleRate.valueOf(sampleRateAdapter.getItem(position).toString())

                frameAdapter.clear()
                frameAdapter.addAll(getFrameSizes(vad.model, vad.sampleRate))
                frameAdapter.notifyDataSetChanged()
                frameSpinner.setSelection(0)

                vad.frameSize = FrameSize.valueOf(frameAdapter.getItem(0).toString())
            }

            SPINNER_FRAME_SIZE_TAG -> {
                vad.frameSize = FrameSize.valueOf(frameAdapter.getItem(position).toString())
            }

            SPINNER_MODE_TAG -> {
                vad.mode = Mode.valueOf(modeAdapter.getItem(position).toString())
            }
        }
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    fun activateRecordingButton() {
        recordingButton.isEnabled = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {}

    override fun onDestroy() {
        super.onDestroy()
        vad.close()
    }
}