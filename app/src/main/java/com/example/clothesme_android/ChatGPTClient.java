package com.example.clothesme_android;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatGPTClient {

    // 날씨 정보를 기반으로 의상 추천을 받는 메소드
    public void getRecommendation(WeatherResponse weatherResponse, final RecommendationCallback callback) {
        // Retrofit 클라이언트 생성
        ApiService apiService = RetrofitClient.getClient(ApiService.BASE_URL).create(ApiService.class);

        // 날씨 정보를 기반으로 프롬프트 구성
        String weatherDescription = weatherResponse.getWeatherDescription();
        double temperature = weatherResponse.getTemp() - 273.15; // 섭씨로 변환
        String prompt = "현재 날씨는 " + weatherDescription + "이고, 온도는 " + temperature + "°C입니다. 이 날씨에 적합한 상의와 하의 코디를 추천해 주세요.";

        // ChatGPTRequest 객체 생성
        ChatGPTRequest request = new ChatGPTRequest(prompt);

        // Retrofit API 호출
        Call<ChatGPTResponse> call = apiService.getWeatherRecommendation(request);
        call.enqueue(new Callback<ChatGPTResponse>() {
            @Override
            public void onResponse(Call<ChatGPTResponse> call, Response<ChatGPTResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 응답 받은 추천 텍스트
                    String recommendation = response.body().getChoices().get(0).getText();
                    callback.onSuccess(recommendation); // 추천 결과 전달
                } else {
                    callback.onFailure("응답 실패: " + response.message()); // 실패 메시지 전달
                }
            }

            @Override
            public void onFailure(Call<ChatGPTResponse> call, Throwable t) {
                callback.onFailure("요청 실패: " + t.getMessage()); // 실패 메시지 전달
            }
        });
    }

    // 콜백 인터페이스 정의 (의상 추천 성공/실패 처리)
    public interface RecommendationCallback {
        void onSuccess(String recommendation);
        void onFailure(String errorMessage);
    }
}
