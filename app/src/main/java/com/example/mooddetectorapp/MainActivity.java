package com.example.mooddetectorapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startFaceDetectionButton = findViewById(R.id.startFaceDetectionButton);
        startFaceDetectionButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FaceDetectionActivity.class);
            startActivity(intent);
        });

        Button startChatButton = findViewById(R.id.startChatButton);
        startChatButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });
    }
}
