package com.example.mooddetectorapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ChatAdapter extends BaseAdapter {

    private Context context;
    private List<ChatMessage> chatMessages;

    public ChatAdapter(Context context, List<ChatMessage> chatMessages) {
        this.context = context;
        this.chatMessages = chatMessages;
    }

    @Override
    public int getCount() {
        return chatMessages.size();
    }

    @Override
    public Object getItem(int position) {
        return chatMessages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage chatMessage = chatMessages.get(position);

        // Check if we need to create a new view or recycle an existing one
        if (convertView == null) {
            // Inflate the appropriate layout for the message sender
            if (chatMessage.getSender().equals("user")) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_message_user, parent, false);
            } else {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_message_bot, parent, false);
            }
        } else {
            // Explicitly set the correct layout for the message sender
            if (chatMessage.getSender().equals("user")) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_message_user, parent, false);
            } else {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_message_bot, parent, false);
            }
        }

        // Bind the message content to the TextView
        TextView messageText = convertView.findViewById(R.id.messageText);
        messageText.setText(chatMessage.getMessage());

        return convertView;
    }
}
