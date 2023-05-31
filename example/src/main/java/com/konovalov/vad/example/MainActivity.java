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
import com.konovalov.vad.config.FrameSize;
import com.konovalov.vad.config.Mode;
import com.konovalov.vad.config.Model;
import com.konovalov.vad.config.SampleRate;
import com.konovalov.vad.models.Vad;
import com.konovalov.vad.models.VadBuilder;
import com.konovalov.vad.example.recorder.VoiceRecorder;
import com.konovalov.vad.Validator;

import java.util.LinkedList;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements
        VoiceRecorder.Listener, View.OnClickListener, AdapterView.OnItemSelectedListener {

    private final Model DEFAULT_MODEL = Model.WEB_RTC_GMM;
    private final SampleRate DEFAULT_SAMPLE_RATE = SampleRate.SAMPLE_RATE_16K;
    private final FrameSize DEFAULT_FRAME_SIZE = FrameSize.FRAME_SIZE_160;
    private final Mode DEFAULT_MODE = Mode.VERY_AGGRESSIVE;

    private final int DEFAULT_SILENCE_DURATION_MS = 300;
    private final int DEFAULT_SPEECH_DURATION_MS = 50;

    private final String SPINNER_MODEL_TAG = "model";
    private final String SPINNER_SAMPLE_RATE_TAG = "sample_rate";
    private final String SPINNER_FRAME_SIZE_TAG = "frame_size";
    private final String SPINNER_MODE_TAG = "mode";

    private FloatingActionButton recordingActionButton;
    private TextView speechTextView;
    private Spinner sampleRateSpinner;
    private Spinner frameSpinner;
    private Spinner modeSpinner;
    private Spinner modelSpinner;

    private ArrayAdapter<String> sampleRateAdapter;
    private ArrayAdapter<String> frameAdapter;
    private ArrayAdapter<String> modeAdapter;
    private ArrayAdapter<String> modelAdapter;

    private VoiceRecorder recorder;
    private Vad vad;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vad = VadBuilder.newBuilder()
                .setModel(DEFAULT_MODEL)
                .setSampleRate(DEFAULT_SAMPLE_RATE)
                .setFrameSize(DEFAULT_FRAME_SIZE)
                .setMode(DEFAULT_MODE)
                .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
                .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
                .setContext(MainActivity.this)
                .build();

        recorder = new VoiceRecorder(this, this);
        recorder.setVad(vad);

        speechTextView = findViewById(R.id.speechTextView);
        sampleRateSpinner = findViewById(R.id.sampleRateSpinner);
        sampleRateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getSampleRates(DEFAULT_MODEL));
        sampleRateSpinner.setAdapter(sampleRateAdapter);
        sampleRateSpinner.setTag(SPINNER_SAMPLE_RATE_TAG);
        sampleRateSpinner.setSelection(getSampleRates(DEFAULT_MODEL).indexOf(DEFAULT_SAMPLE_RATE.name()), false);
        sampleRateSpinner.setOnItemSelectedListener(this);

        frameSpinner = findViewById(R.id.frameSampleRateSpinner);
        frameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getFrameSizes(DEFAULT_MODEL, DEFAULT_SAMPLE_RATE));
        frameSpinner.setAdapter(frameAdapter);
        frameSpinner.setTag(SPINNER_FRAME_SIZE_TAG);
        frameSpinner.setSelection(getFrameSizes(DEFAULT_MODEL, DEFAULT_SAMPLE_RATE).indexOf(DEFAULT_FRAME_SIZE.name()), false);
        frameSpinner.setOnItemSelectedListener(this);

        modeSpinner = findViewById(R.id.modeSpinner);
        modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getModes());
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setTag(SPINNER_MODE_TAG);
        modeSpinner.setSelection(getModes().indexOf(DEFAULT_MODE.name()), false);
        modeSpinner.setOnItemSelectedListener(this);

        modelSpinner = findViewById(R.id.modelSpinner);
        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getModels());
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setTag(SPINNER_MODEL_TAG);
        modelSpinner.setSelection(getModes().indexOf(DEFAULT_MODEL.name()), false);
        modelSpinner.setOnItemSelectedListener(this);

        recordingActionButton = findViewById(R.id.recordingActionButton);
        recordingActionButton.setOnClickListener(this);
        recordingActionButton.setEnabled(false);

        MainActivityPermissionsDispatcher.activateAudioPermissionWithPermissionCheck(this);
    }

    private LinkedList<String> getSampleRates(Model model) {
        LinkedList<String> result = new LinkedList<>();

        for (SampleRate sampleRate : Validator.getValidSampleRates(model)) {
            result.add(sampleRate.name());
        }

        return result;
    }

    private LinkedList<String> getFrameSizes(Model model, SampleRate sampleRate) {
        LinkedList<String> result = new LinkedList<>();

        for (FrameSize frameSize : Validator.getValidFrameSizes(model, sampleRate)) {
            result.add(frameSize.name());
        }

        return result;
    }

    private LinkedList<String> getModes() {
        LinkedList<String> result = new LinkedList<>();

        for (Mode mode : Mode.values()) {
            result.add(mode.name());
        }

        return result;
    }

    private LinkedList<String> getModels() {
        LinkedList<String> result = new LinkedList<>();

        for (Model modelType : Model.values()) {
            result.add(modelType.name());
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
            case SPINNER_MODEL_TAG:
                Model model = Model.valueOf(String.valueOf(modelAdapter.getItem(position)));

                sampleRateAdapter.clear();
                sampleRateAdapter.addAll(getSampleRates(model));
                sampleRateAdapter.notifyDataSetChanged();
                sampleRateSpinner.setSelection(0);

                SampleRate sampleRate = SampleRate.valueOf(String.valueOf(sampleRateAdapter.getItem(0)));

                frameAdapter.clear();
                frameAdapter.addAll(getFrameSizes(model, sampleRate));
                frameAdapter.notifyDataSetChanged();
                frameSpinner.setSelection(0);

                FrameSize frameSize = FrameSize.valueOf(String.valueOf(frameAdapter.getItem(0)));

                modeSpinner.setSelection(getModes().indexOf(DEFAULT_MODE.name()), false);

                vad.close();
                vad = VadBuilder.newBuilder()
                        .setModel(model)
                        .setSampleRate(sampleRate)
                        .setFrameSize(frameSize)
                        .setMode(DEFAULT_MODE)
                        .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
                        .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
                        .setContext(MainActivity.this)
                        .build();

                recorder.setVad(vad);
                break;
            case SPINNER_SAMPLE_RATE_TAG:
                vad.setSampleRate(SampleRate.valueOf(String.valueOf(sampleRateAdapter.getItem(position))));

                frameAdapter.clear();
                frameAdapter.addAll(getFrameSizes(vad.getModel(), vad.getSampleRate()));
                frameAdapter.notifyDataSetChanged();
                frameSpinner.setSelection(0);

                vad.setFrameSize(FrameSize.valueOf(String.valueOf(frameAdapter.getItem(0))));
                break;
            case SPINNER_FRAME_SIZE_TAG:
                vad.setFrameSize(FrameSize.valueOf(String.valueOf(frameAdapter.getItem(position))));
                break;
            case SPINNER_MODE_TAG:
                vad.setMode(Mode.valueOf(String.valueOf(modeAdapter.getItem(position))));
                break;
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (vad != null) {
            vad.close();
        }
    }
}
