package com.example.mooddetectorapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class TaskAdapter extends BaseAdapter {
    private Context context;
    private List<Task> tasks;

    public TaskAdapter(Context context, List<Task> tasks) {
        this.context = context;
        this.tasks = tasks;
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public Object getItem(int position) {
        return tasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Task task = tasks.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        }

        TextView titleTextView = convertView.findViewById(R.id.taskTitleTextView);
        TextView descriptionTextView = convertView.findViewById(R.id.taskDescriptionTextView);
        TextView pointsTextView = convertView.findViewById(R.id.taskPointsTextView);
        TextView statusTextView = convertView.findViewById(R.id.taskStatusTextView);

        titleTextView.setText(task.getTitle());
        descriptionTextView.setText(task.getDescription());
        pointsTextView.setText(String.valueOf(task.getRewardPoints()));
        statusTextView.setText(task.getStatus());

        return convertView;
    }
}
