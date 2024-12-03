package com.example.clothesme_android;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class ChatGPTRequest {
    @SerializedName("model")
    private String model;

    @SerializedName("messages")
    private List<Message> messages;

    public ChatGPTRequest(List<Message> messages) {
        this.model = "gpt-3.5-turbo";
        this.messages = messages;
    }

    public static class Message {
        @SerializedName("role")
        private String role;

        @SerializedName("content")
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
