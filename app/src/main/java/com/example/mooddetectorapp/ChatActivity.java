package com.example.mooddetectorapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private ListView chatListView;
    private EditText messageEditText;
    private Button sendButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private OpenAIService openAIService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chat");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        chatListView = findViewById(R.id.chatListView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages);
        chatListView.setAdapter(chatAdapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadConversation();
        }

        openAIService = ApiClient.getClient().create(OpenAIService.class);

        sendButton.setOnClickListener(v -> {
            String userMessage = messageEditText.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                sendMessage(userMessage);
            }
        });
    }


    private void sendMessage(String message) {
        chatMessages.add(new ChatMessage("user", message));
        chatAdapter.notifyDataSetChanged();
        messageEditText.setText("");

        List<OpenAIChatRequest.Message> messages = new ArrayList<>();
        for (ChatMessage chatMessage : chatMessages) {
            String role = chatMessage.getSender().equals("user") ? "user" : "assistant";
            messages.add(new OpenAIChatRequest.Message(role, chatMessage.getMessage()));
        }

        messages.add(new OpenAIChatRequest.Message("system", "You are a friendly and supportive chatbot. Act like a good friend and remember previous interactions. Use positive, encouraging, and empathetic language. Make the user feels understood and supported."));

        OpenAIChatRequest request = new OpenAIChatRequest("gpt-3.5-turbo-16k", messages, 150, 0.7);
        Call<OpenAIResponse> call = openAIService.generateResponse(request);

        call.enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(Call<OpenAIResponse> call, Response<OpenAIResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String botResponse = response.body().getChoices().get(0).getMessage().getContent().trim();
                    chatMessages.add(new ChatMessage("assistant", botResponse));
                    chatAdapter.notifyDataSetChanged();
                    saveConversation();
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                Log.e(TAG, "Failure: " + t.getMessage(), t);
                Toast.makeText(ChatActivity.this, "Failed to communicate with bot", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveConversation() {
        if (currentUser != null) {
            String uid = currentUser.getUid();
            Map<String, Object> chatData = new HashMap<>();
            chatData.put("chatMessages", new Gson().toJson(chatMessages));
            db.collection("users").document(uid).collection("chat").document("chatHistory").set(chatData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Chat history successfully written!"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error writing chat history", e));
        }
    }


    private void loadConversation() {
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).collection("chat").document("chatHistory").get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String json = document.getString("chatMessages");
                                Type type = new TypeToken<List<ChatMessage>>() {}.getType();
                                chatMessages = new Gson().fromJson(json, type);
                                if (chatMessages == null) {
                                    chatMessages = new ArrayList<>();
                                }
                                chatAdapter = new ChatAdapter(ChatActivity.this, chatMessages);
                                chatListView.setAdapter(chatAdapter);
                                chatAdapter.notifyDataSetChanged();
                            }
                        } else {
                            Log.d(TAG, "Failed to retrieve chat history");
                        }
                    });
        }
    }


    private void handleApiError(Response<OpenAIResponse> response) {
        try {
            String errorBody = response.errorBody().string();
            JSONObject jsonObject = new JSONObject(errorBody);
            String errorMessage = jsonObject.getJSONObject("error").getString("message");
            Log.e(TAG, "Error: " + errorMessage);
            Toast.makeText(ChatActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response: " + e.getMessage(), e);
            Toast.makeText(ChatActivity.this, "An unknown error occurred", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveConversation();
    }

}
