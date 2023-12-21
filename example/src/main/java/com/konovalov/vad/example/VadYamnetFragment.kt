package com.konovalov.vad.example

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.konovalov.vad.example.recorder.VoiceRecorder
import com.konovalov.vad.example.recorder.VoiceRecorder.AudioCallback
import com.konovalov.vad.yamnet.Vad
import com.konovalov.vad.yamnet.VadYamnet
import com.konovalov.vad.yamnet.config.FrameSize
import com.konovalov.vad.yamnet.config.Mode
import com.konovalov.vad.yamnet.config.SampleRate
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class VadYamnetFragment : Fragment(),
    AudioCallback,
    View.OnClickListener,
    AdapterView.OnItemSelectedListener {

    private val DEFAULT_SAMPLE_RATE = SampleRate.SAMPLE_RATE_16K
    private val DEFAULT_FRAME_SIZE = FrameSize.FRAME_SIZE_243
    private val DEFAULT_MODE = Mode.NORMAL
    private val DEFAULT_SILENCE_DURATION_MS = 30
    private val DEFAULT_SPEECH_DURATION_MS = 30

    private val SPINNER_SAMPLE_RATE_TAG = "sample_rate"
    private val SPINNER_FRAME_SIZE_TAG = "frame_size"
    private val SPINNER_MODE_TAG = "mode"

    private lateinit var titleTextView: TextView

    private lateinit var recordingButton: FloatingActionButton
    private lateinit var speechTextView: TextView

    private lateinit var sampleRateSpinner: Spinner
    private lateinit var frameSpinner: Spinner
    private lateinit var modeSpinner: Spinner

    private lateinit var sampleRateAdapter: ArrayAdapter<String>
    private lateinit var frameAdapter: ArrayAdapter<String>
    private lateinit var modeAdapter: ArrayAdapter<String>

    private lateinit var recorder: VoiceRecorder
    private lateinit var vad: VadYamnet
    private var isRecording = false

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_vad_main, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vad = Vad.builder()
            .setContext(requireContext())
            .setSampleRate(DEFAULT_SAMPLE_RATE)
            .setFrameSize(DEFAULT_FRAME_SIZE)
            .setMode(DEFAULT_MODE)
            .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
            .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
            .build()

        recorder = VoiceRecorder(this)

        titleTextView = view.findViewById(R.id.titleTextView)
        titleTextView.setText(R.string.vad_yamnet)

        speechTextView = view.findViewById(R.id.speechTextView)
        sampleRateSpinner = view.findViewById(R.id.sampleRateSpinner)
        sampleRateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, getSampleRates())
        sampleRateSpinner.adapter = sampleRateAdapter
        sampleRateSpinner.tag = SPINNER_SAMPLE_RATE_TAG
        sampleRateSpinner.setSelection(getSampleRates().indexOf(DEFAULT_SAMPLE_RATE.name), false)
        sampleRateSpinner.onItemSelectedListener = this

        frameSpinner = view.findViewById(R.id.frameSampleRateSpinner)
        frameAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, getFrameSizes(DEFAULT_SAMPLE_RATE))
        frameSpinner.adapter = frameAdapter
        frameSpinner.tag = SPINNER_FRAME_SIZE_TAG
        frameSpinner.setSelection(getFrameSizes(DEFAULT_SAMPLE_RATE).indexOf(DEFAULT_FRAME_SIZE.name), false)
        frameSpinner.onItemSelectedListener = this

        modeSpinner = view.findViewById(R.id.modeSpinner)
        modeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, modes())
        modeSpinner.adapter = modeAdapter
        modeSpinner.tag = SPINNER_MODE_TAG
        modeSpinner.setSelection(modes().indexOf(DEFAULT_MODE.name), false)
        modeSpinner.onItemSelectedListener = this

        recordingButton = view.findViewById(R.id.recordingActionButton)
        recordingButton.setOnClickListener(this)
        recordingButton.isEnabled = false

        activateRecordingButtonWithPermissionCheck()
    }

    override fun onAudio(audioData: ShortArray) {
        val speech = "Speech"
        val soundCategory = vad.classifyAudio(speech, audioData)

        requireActivity().runOnUiThread{
            when (soundCategory.label) {
                speech -> speechTextView.setText(R.string.speech_detected)
                else -> speechTextView.setText(R.string.noise_detected)
            }
        }
    }

    private fun getSampleRates(): List<String> {
        return SampleRate.values().map { it.name }.toList()
    }

    private fun getFrameSizes(sampleRate: SampleRate): List<String> {
        return vad.supportedParameters.get(sampleRate)?.map { it.name }?.toList() ?: emptyList()
    }

    private fun modes(): List<String> {
        return Mode.values().map { it.name }.toList()
    }

    private fun startRecording() {
        isRecording = true
        recorder.start(vad.sampleRate.value, vad.frameSize.value)
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
            SPINNER_SAMPLE_RATE_TAG -> {
                vad.sampleRate = SampleRate.valueOf(sampleRateAdapter.getItem(position).toString())

                frameAdapter.clear()
                frameAdapter.addAll(getFrameSizes(vad.sampleRate))
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

    override fun onDestroyView() {
        super.onDestroyView()
        recorder.stop()
        vad.close()
    }
}