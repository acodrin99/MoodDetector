package com.example.mooddetectorapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView welcomeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        welcomeTextView = findViewById(R.id.welcome_message);

        Button startFaceDetectionButton = findViewById(R.id.btn_start_face_detection);
        startFaceDetectionButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FaceDetectionActivity.class);
            startActivity(intent);
        });

        Button startChatButton = findViewById(R.id.btn_start_chat);
        startChatButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        Button infoButton = findViewById(R.id.btn_info);
        infoButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, InfoActivity.class);
            startActivity(intent);
        });

        Button rewardButton = findViewById(R.id.btn_rewards);
        rewardButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RewardActivity.class);
            startActivity(intent);
        });

        Button logoutButton = findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        Button btnVoiceDetection = findViewById(R.id.btn_start_voice_detection);
        btnVoiceDetection.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VoiceDetectionActivity.class);
            startActivity(intent);
        });

        updateWelcomeMessage();
    }

    private void updateWelcomeMessage() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    String name = document.getString("name");
                    if (name != null && !name.isEmpty()) {
                        welcomeTextView.setText("Welcome, " + name + "!");
                    } else {
                        String email = currentUser.getEmail();
                        if (email != null) {
                            String username = email.split("@")[0];
                            welcomeTextView.setText("Welcome, " + username + "!");
                        } else {
                            welcomeTextView.setText("Welcome!");
                        }
                    }
                } else {
                    welcomeTextView.setText("Welcome!");
                }
            });
        } else {
            welcomeTextView.setText("Welcome!");
        }
    }
}
