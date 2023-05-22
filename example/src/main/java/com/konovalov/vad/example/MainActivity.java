package com.konovalov.vad.example;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.konovalov.vad.Vad;
import com.konovalov.vad.VadConfig;
import com.konovalov.vad.example.recorder.VoiceRecorder;

import java.util.LinkedList;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements VoiceRecorder.Listener, View.OnClickListener, AdapterView.OnItemSelectedListener {

    private final VadConfig.SampleRate DEFAULT_SAMPLE_RATE = VadConfig.SampleRate.SAMPLE_RATE_16K;
    private final VadConfig.FrameSize DEFAULT_FRAME_SIZE = VadConfig.FrameSize.FRAME_SIZE_160;
    private final VadConfig.Mode DEFAULT_MODE = VadConfig.Mode.VERY_AGGRESSIVE;

    private final int DEFAULT_SILENCE_DURATION = 500;
    private final int DEFAULT_VOICE_DURATION = 500;

    private final String SPINNER_SAMPLE_RATE_TAG = "sample_rate";
    private final String SPINNER_FRAME_SIZE_TAG = "frame_size";
    private final String SPINNER_MODE_TAG = "mode";

    private FloatingActionButton recordingActionButton;
    private TextView speechTextView;
    private Spinner sampleRateSpinner;
    private Spinner frameSpinner;
    private Spinner modeSpinner;

    private ArrayAdapter<String> sampleRateAdapter;
    private ArrayAdapter<String> frameAdapter;
    private ArrayAdapter<String> modeAdapter;

    private VoiceRecorder recorder;
    private VadConfig config;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        config = VadConfig.newBuilder()
                .setSampleRate(DEFAULT_SAMPLE_RATE)
                .setFrameSize(DEFAULT_FRAME_SIZE)
                .setMode(DEFAULT_MODE)
                .setSilenceDurationMillis(DEFAULT_SILENCE_DURATION)
                .setVoiceDurationMillis(DEFAULT_VOICE_DURATION)
                .build();

        recorder = new VoiceRecorder(this,this, config);

        speechTextView = findViewById(R.id.speechTextView);
        sampleRateSpinner = findViewById(R.id.sampleRateSpinner);
        sampleRateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getSampleRates());
        sampleRateSpinner.setAdapter(sampleRateAdapter);
        sampleRateSpinner.setTag(SPINNER_SAMPLE_RATE_TAG);
        sampleRateSpinner.setSelection(getSampleRates().indexOf(DEFAULT_SAMPLE_RATE.name()), false);
        sampleRateSpinner.setOnItemSelectedListener(this);

        frameSpinner = findViewById(R.id.frameSampleRateSpinner);
        frameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getFrameSizes());
        frameSpinner.setAdapter(frameAdapter);
        frameSpinner.setTag(SPINNER_FRAME_SIZE_TAG);
        frameSpinner.setSelection(getFrameSizes().indexOf(DEFAULT_FRAME_SIZE.name()), false);
        frameSpinner.setOnItemSelectedListener(this);

        modeSpinner = findViewById(R.id.modeSpinner);
        modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getModes());
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setTag(SPINNER_MODE_TAG);
        modeSpinner.setSelection(getModes().indexOf(DEFAULT_MODE.name()), false);
        modeSpinner.setOnItemSelectedListener(this);

        recordingActionButton = findViewById(R.id.recordingActionButton);
        recordingActionButton.setOnClickListener(this);
        recordingActionButton.setEnabled(false);

        MainActivityPermissionsDispatcher.activateAudioPermissionWithPermissionCheck(this);
    }

    private LinkedList<String> getSampleRates() {
        LinkedList<String> result = new LinkedList<>();
        for (VadConfig.SampleRate sampleRate : VadConfig.SampleRate.values()) {
            result.add(sampleRate.name());
        }
        return result;
    }

    private LinkedList<String> getFrameSizes() {
        LinkedList<String> result = new LinkedList<>();
        LinkedList<VadConfig.FrameSize> supportingFrameSizes = Vad.getValidFrameSize(config.getSampleRate());

        if (supportingFrameSizes != null) {
            for (VadConfig.FrameSize frameSize : supportingFrameSizes) {
                result.add(frameSize.name());
            }
        }

        return result;
    }

    private LinkedList<String> getModes() {
        LinkedList<String> result = new LinkedList<>();
        for (VadConfig.Mode mode : VadConfig.Mode.values()) {
            result.add(mode.name());
        }
        return result;
    }

    private void startRecording() {
        isRecording = true;
        recorder.start();
        recordingActionButton.setImageResource(R.drawable.stop);
    }

    private void stopRecording() {
        isRecording = false;
        recorder.stop();
        recordingActionButton.setImageResource(R.drawable.red_dot);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        stopRecording();

        switch (String.valueOf(adapterView.getTag())) {
            case SPINNER_SAMPLE_RATE_TAG:
                config.setSampleRate(VadConfig.SampleRate.valueOf(String.valueOf(sampleRateAdapter.getItem(position))));

                frameAdapter.clear();
                frameAdapter.addAll(getFrameSizes());
                frameAdapter.notifyDataSetChanged();
                frameSpinner.setSelection(0);

                config.setFrameSize(VadConfig.FrameSize.valueOf(String.valueOf(frameAdapter.getItem(0))));
                break;
            case SPINNER_FRAME_SIZE_TAG:
                config.setFrameSize(VadConfig.FrameSize.valueOf(String.valueOf(frameAdapter.getItem(position))));
                break;
            case SPINNER_MODE_TAG:
                config.setMode(VadConfig.Mode.valueOf(String.valueOf(modeAdapter.getItem(position))));
                break;
        }

        recorder.updateConfig(config);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    public void activateAudioPermission() {
        recordingActionButton.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    @Override
    public void onSpeechDetected() {
        runOnUiThread(() -> speechTextView.setText(R.string.speech_detected));
    }

    @Override
    public void onNoiseDetected() {
        runOnUiThread(() -> speechTextView.setText(R.string.noise_detected));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }


}
