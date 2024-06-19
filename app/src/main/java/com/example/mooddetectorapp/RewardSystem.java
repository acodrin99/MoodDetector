package com.example.mooddetectorapp;

import java.util.ArrayList;
import java.util.List;

public class RewardSystem {
    private List<Task> tasks;
    private int totalPoints;

    public RewardSystem() {
        tasks = new ArrayList<>();
        totalPoints = 0;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void completeTask(Task task) {
        if (!task.isCompleted()) {
            task.setCompleted(true);
            totalPoints += task.getRewardPoints();
        }
    }

    public void uncompleteTask(Task task) {
        if (task.isCompleted()) {
            task.setCompleted(false);
            totalPoints -= task.getRewardPoints();
        }
    }

    public void toggleTaskCompletion(Task task) {
        if (task.isCompleted()) {
            uncompleteTask(task);
        } else {
            completeTask(task);
        }
    }

    public void addPoints(int points) {
        this.totalPoints += points;
    }

    public boolean taskExists(String title) {
        for (Task task : tasks) {
            if (task.getTitle().equals(title)) {
                return true;
            }
        }
        return false;
    }

    public Task getTaskByTitle(String title) {
        for (Task task : tasks) {
            if (task.getTitle().equals(title)) {
                return task;
            }
        }
        return null;
    }
}
