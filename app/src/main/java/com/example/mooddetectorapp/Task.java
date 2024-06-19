package com.example.mooddetectorapp;

import com.google.firebase.firestore.Exclude;

public class Task {
    private String title;
    private String description;
    private int rewardPoints;
    private boolean completed;

    // Default constructor required for calls to DataSnapshot.getValue(Task.class)
    public Task() {}

    public Task(String title, String description, int rewardPoints) {
        this.title = title;
        this.description = description;
        this.rewardPoints = rewardPoints;
        this.completed = false;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Exclude
    public String getStatus() {
        return completed ? "Completed" : "Not Completed";
    }
}
