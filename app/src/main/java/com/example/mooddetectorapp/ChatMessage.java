package com.example.mooddetectorapp;

public class ChatMessage {
    private String sender;
    private String message;

    public ChatMessage() {
        // No-argument constructor needed for Firestore deserialization
    }

    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
