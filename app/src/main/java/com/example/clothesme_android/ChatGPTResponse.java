package com.example.clothesme_android;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatGPTResponse {
    @SerializedName("choices")
    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public static class Choice {
        @SerializedName("text")
        private String text;

        public String getText() {
            return text;
        }
    }
}
