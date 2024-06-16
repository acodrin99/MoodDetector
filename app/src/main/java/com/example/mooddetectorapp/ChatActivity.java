package com.example.mooddetectorapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private ListView chatListView;
    private EditText messageEditText;
    private Button sendButton;
    private Button eraseButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private OpenAIService openAIService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        chatListView = findViewById(R.id.chatListView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        eraseButton = findViewById(R.id.eraseButton);

        loadConversation();

        chatAdapter = new ChatAdapter(this, chatMessages);
        chatListView.setAdapter(chatAdapter);

        openAIService = ApiClient.getClient().create(OpenAIService.class);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = messageEditText.getText().toString().trim();
                if (!userMessage.isEmpty()) {
                    sendMessage(userMessage);
                }
            }
        });

        eraseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatMessages.clear();
                chatAdapter.notifyDataSetChanged();
                saveConversation();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

        messages.add(new OpenAIChatRequest.Message("system", "You are a friendly and supportive chatbot. Act like a good friend and remember previous interactions."));

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
        SharedPreferences sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(chatMessages);
        editor.putString("chat_history", json);
        editor.apply();
    }

    private void loadConversation() {
        SharedPreferences sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("chat_history", null);
        Type type = new TypeToken<List<ChatMessage>>() {}.getType();
        chatMessages = gson.fromJson(json, type);
        if (chatMessages == null) {
            chatMessages = new ArrayList<>();
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
