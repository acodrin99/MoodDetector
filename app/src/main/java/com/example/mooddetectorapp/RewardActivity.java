package com.example.mooddetectorapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class RewardActivity extends AppCompatActivity {

    private static final String TAG = "RewardActivity";
    private static final int SMILE_REQUEST_CODE = 1;
    private static final int CURRENT_TASK_VERSION = 2;

    private RewardSystem rewardSystem;
    private TaskAdapter taskAdapter;
    private TextView totalPointsTextView;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Task smileTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reward System");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        totalPointsTextView = findViewById(R.id.totalPointsTextView);
        ListView taskListView = findViewById(R.id.taskListView);

        rewardSystem = new RewardSystem();
        taskAdapter = new TaskAdapter(this, rewardSystem.getTasks());
        taskListView.setAdapter(taskAdapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadUserData();
        }

        taskListView.setOnItemClickListener((parent, view, position, id) -> {
            Task task = rewardSystem.getTasks().get(position);
            if (task.getTitle().equals("Smile for the Camera")) {
                if (!task.isCompleted()) {
                    Intent intent = new Intent(RewardActivity.this, FaceDetectionActivity.class);
                    intent.putExtra("fromRewardTask", true);
                    startActivityForResult(intent, SMILE_REQUEST_CODE);
                } else {
                    rewardSystem.toggleTaskCompletion(task);
                    taskAdapter.notifyDataSetChanged();
                    updateTotalPoints();
                    saveUserData();
                    Toast.makeText(RewardActivity.this, "Smile task uncompleted. You lost " + task.getRewardPoints() + " points.", Toast.LENGTH_SHORT).show();
                }
            } else {
                rewardSystem.toggleTaskCompletion(task);
                taskAdapter.notifyDataSetChanged();
                updateTotalPoints();
                saveUserData();
                if (task.isCompleted()) {
                    Toast.makeText(RewardActivity.this, "Task completed! You earned " + task.getRewardPoints() + " points.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RewardActivity.this, "Task uncompleted. You lost " + task.getRewardPoints() + " points.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateTotalPoints();
    }

    private void initializeTasks() {
        rewardSystem.addTask(new Task("Practice Mindfulness", "Spend 10 minutes practicing mindfulness.", 10));
        rewardSystem.addTask(new Task("Exercise", "Do 30 minutes of physical activity.", 20));
        rewardSystem.addTask(new Task("Healthy Meal", "Prepare and eat a healthy meal.", 15));
        rewardSystem.addTask(new Task("Sleep Well", "Get at least 8 hours of sleep.", 25));
        rewardSystem.addTask(new Task("Talk to a Friend", "Have a meaningful conversation with a friend.", 10));
        rewardSystem.addTask(new Task("Smile for the Camera", "Smile to the camera to earn points.", 30));
        taskAdapter.notifyDataSetChanged();
    }

    private void updateTotalPoints() {
        totalPointsTextView.setText("Total Points: " + rewardSystem.getTotalPoints());
    }

    private void saveUserData() {
        if (currentUser != null) {
            String uid = currentUser.getUid();
            Map<String, Object> userData = new HashMap<>();
            userData.put("totalPoints", rewardSystem.getTotalPoints());
            userData.put("taskVersion", CURRENT_TASK_VERSION);
            db.collection("users").document(uid).set(userData, SetOptions.merge());

            // Save task completion status
            for (Task task : rewardSystem.getTasks()) {
                db.collection("users").document(uid).collection("tasks").document(task.getTitle()).set(task);
            }
        }
    }

    private void loadUserData() {
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Long points = document.getLong("totalPoints");
                        if (points != null) {
                            rewardSystem.setTotalPoints(points.intValue());
                            updateTotalPoints();
                        }
                        Long version = document.getLong("taskVersion");
                        if (version == null || version < CURRENT_TASK_VERSION) {
                            initializeTasks();
                            saveUserData();
                        }
                    }
                } else {
                    Log.d(TAG, "Failed to retrieve user data");
                }
            });

            db.collection("users").document(uid).collection("tasks").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    if (task.getResult().isEmpty()) {
                        // If no tasks are found, initialize default tasks
                        initializeTasks();
                        saveUserData(); // Save the initialized tasks to Firestore
                    } else {
                        for (DocumentSnapshot document : task.getResult()) {
                            Task taskData = document.toObject(Task.class);
                            if (taskData != null) {
                                rewardSystem.addTask(taskData);
                            }
                        }
                        taskAdapter.notifyDataSetChanged();
                    }
                } else {
                    Log.d(TAG, "Failed to retrieve tasks");
                    initializeTasks();
                    saveUserData(); // Save the initialized tasks to Firestore
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SMILE_REQUEST_CODE && resultCode == RESULT_OK) {
            smileTask = rewardSystem.getTaskByTitle("Smile for the Camera");
            if (smileTask != null && !smileTask.isCompleted()) {
                rewardSystem.toggleTaskCompletion(smileTask);
                taskAdapter.notifyDataSetChanged();
                updateTotalPoints();
                saveUserData();
                Toast.makeText(this, "Great! You smiled and earned " + smileTask.getRewardPoints() + " points.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
