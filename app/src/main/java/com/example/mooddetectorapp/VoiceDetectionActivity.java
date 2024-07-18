package com.example.mooddetectorapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

public class VoiceDetectionActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 16000;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private AudioRecord recorder;
    private boolean isRecording = false;
    private VoiceEmotionDetector voiceEmotionDetector;
    private TextView emotionResultTextView;
    private Button startDetectionButton;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_detection);

        emotionResultTextView = findViewById(R.id.emotionResultTextView);
        startDetectionButton = findViewById(R.id.startDetectionButton);

        try {
            voiceEmotionDetector = new VoiceEmotionDetector(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        startDetectionButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                startDetectionButton.setText("Start Emotion Detection");
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
                } else {
                    startRecording();
                    startDetectionButton.setText("Stop Emotion Detection");
                }
            }
        });
    }

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT));

        recorder.startRecording();
        isRecording = true;
        new Thread(() -> {
            float[] audioData = new float[128]; // Example buffer size
            while (isRecording) {
                int read = 0;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    read = recorder.read(audioData, 0, audioData.length, AudioRecord.READ_NON_BLOCKING);
                }
                if (read > 0) {
                    String emotion = voiceEmotionDetector.detectEmotion(audioData);
                    runOnUiThread(() -> emotionResultTextView.setText("Detected Emotion: " + emotion));
                }
            }
        }).start();
    }

    private void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
                startDetectionButton.setText("Stop Emotion Detection");
            } else {
                // Permission denied, disable functionality that depends on this permission.
                emotionResultTextView.setText("Permission to record audio is required for emotion detection.");
            }
        }
    }
}
