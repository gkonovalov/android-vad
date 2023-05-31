package com.konovalov.vad.example.recorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import com.konovalov.vad.models.VadListener;
import com.konovalov.vad.models.Vad;

import androidx.core.app.ActivityCompat;

import static android.media.AudioFormat.CHANNEL_IN_MONO;

/**
 * Created by George Konovalov on 11/16/2019.
 */
public class VoiceRecorder {
    private static final int PCM_ENCODING_BIT = AudioFormat.ENCODING_PCM_16BIT;

    private Vad vad;
    private final Listener callback;
    private final Context context;
    private AudioRecord audioRecord;
    private Thread thread;

    private boolean isListening = false;

    private static final String TAG = VoiceRecorder.class.getSimpleName();

    public VoiceRecorder(Context context, Listener callback) {
        this.context = context;
        this.callback = callback;
    }

    public void setVad(Vad vad) {
        this.vad = vad;
    }

    public void start() {
        audioRecord = createAudioRecord();
        if (audioRecord != null) {
            isListening = true;
            audioRecord.startRecording();

            thread = new Thread(new ProcessVoice());
            thread.start();
        } else {
            Log.w(TAG, "Failed start Voice Recorder!");
        }
    }

    public void stop() {
        isListening = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (audioRecord != null) {
            try {
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stop AudioRecord ", e);
            }
            audioRecord = null;
        }
    }

    private AudioRecord createAudioRecord() {
        try {
            final int minBufSize = AudioRecord.getMinBufferSize(
                    vad.getSampleRate().getValue(),
                    CHANNEL_IN_MONO,
                    PCM_ENCODING_BIT);

            if (minBufSize == AudioRecord.ERROR_BAD_VALUE) {
                return null;
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    vad.getSampleRate().getValue(),
                    CHANNEL_IN_MONO,
                    PCM_ENCODING_BIT,
                    minBufSize);

            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                return audioRecord;
            } else {
                audioRecord.release();
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error can't create AudioRecord ", e);
        }

        return null;
    }

    private class ProcessVoice implements Runnable {

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            int size = vad.getFrameSize().getValue();

            while (!Thread.interrupted() && isListening && audioRecord != null) {
                short[] buffer = new short[size];

                audioRecord.read(buffer, 0, buffer.length);

                detectSpeech(buffer);
            }
        }

        private void detectSpeech(short[] buffer) {
            vad.setContinuousSpeechListener(buffer, new VadListener() {
                @Override
                public void onSpeechDetected() {
                    callback.onSpeechDetected();
                }

                @Override
                public void onNoiseDetected() {
                    callback.onNoiseDetected();
                }
            });
        }
    }

    public interface Listener {
        void onSpeechDetected();

        void onNoiseDetected();
    }

}
