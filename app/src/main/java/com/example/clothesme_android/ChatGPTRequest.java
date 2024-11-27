package com.example.clothesme_android;

import com.google.gson.annotations.SerializedName;

public class ChatGPTRequest {
    @SerializedName("model") // ChatGPT 모델 이름
    private String model;

    @SerializedName("prompt") // 요청 메세지
    private String prompt;

    @SerializedName("max_tokens") // 응답 최대 길이
    private int maxTokens;

    @SerializedName("temperature") // 출력 창의성
    private double temperature;

    public ChatGPTRequest(String prompt) {
        this.model = "gpt-3.5-turbo";  // 모델명은 기본적으로 GPT-3.5로 설정
        this.prompt = prompt;
        this.maxTokens = 100;  // 응답 최대 길이 (예시로 100으로 설정)
        this.temperature = 0.7;  // 창의성 정도 (0~1 범위)
    }

    public String getModel() {
        return model;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }
}
