package com.example.clothesme_android;

import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Arrays;
import java.util.List;

public class ChatGPTClient {
    private static final String BASE_URL = "https://api.openai.com/v1/";
    private ApiService apiService;

    public ChatGPTClient() {
        // OkHttpClient에 Interceptor 추가
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", "Bearer " + BuildConfig.OPENAI_API_KEY)
                            .header("Content-Type", "application/json");
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .build();

        // Retrofit 클라이언트 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client) // Interceptor가 추가된 OkHttpClient 사용
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    // 날씨 정보를 기반으로 의상 추천을 받는 메서드
    public void getRecommendation(WeatherResponse weatherResponse, final RecommendationCallback callback) {
        List<ChatGPTRequest.Message> messages = createMessagesFromWeather(weatherResponse);
        ChatGPTRequest chatGPTRequest = new ChatGPTRequest(messages);

        apiService.getRecommendation(chatGPTRequest).enqueue(new retrofit2.Callback<ChatGPTResponse>() {
            @Override
            public void onResponse(Call<ChatGPTResponse> call, retrofit2.Response<ChatGPTResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("ChatGPTResponse", "Response Body: " + response.body());
                    Log.d("ChatGPTResponse", new Gson().toJson(response.body()));
                    if (!response.body().getChoices().isEmpty()) {
                        String recommendation = response.body().getChoices().get(0).getMessage().getContent();
                        callback.onSuccess(recommendation);
                    } else {
                        callback.onFailure("추천을 가져오는 데 실패했습니다. 응답이 비어있습니다.");
                    }
                } else {
                    Log.e("ChatGPTClient", "Response Error: " + response.code());
                    callback.onFailure("추천을 가져오는 데 실패했습니다.");
                }
            }

            @Override
            public void onFailure(Call<ChatGPTResponse> call, Throwable t) {
                if (t instanceof retrofit2.HttpException) {
                    retrofit2.HttpException httpException = (retrofit2.HttpException) t;
                    if (httpException.code() == 429) {
                        String retryAfter = httpException.response().headers().get("Retry-After");
                        long retryTime = retryAfter != null ? Long.parseLong(retryAfter) : 5;
                        new Handler().postDelayed(() -> getRecommendation(weatherResponse, callback), retryTime * 1000);
                    } else {
                        callback.onFailure("네트워크 오류가 발생했습니다.");
                    }
                } else {
                    callback.onFailure("네트워크 오류가 발생했습니다.");
                }
            }
        });
    }

    // WeatherResponse를 기반으로 messages 리스트 생성
    private List<ChatGPTRequest.Message> createMessagesFromWeather(WeatherResponse weatherResponse) {
        String weatherDescription = weatherResponse.getWeather()[0].getDescription();
        String temperature = (int) weatherResponse.getMain().getTemp() + "°C";
        String humidity = weatherResponse.getMain().getHumidity() + "%";

        String userPrompt = String.format("현재 날씨는 %s이고 온도는 %s, 습도는 %s입니다. 이 조건에 적합한 상하의 옷 종류와 색상을 고려한 코디를 50자 이내로 추천해주세요.",
                weatherDescription, temperature, humidity);

        return Arrays.asList(
                new ChatGPTRequest.Message("system", "You are a helpful fashion assistant who recommends outfits based on the weather."),
                new ChatGPTRequest.Message("user", userPrompt)
        );
    }

    // 추천 결과 콜백 인터페이스
    public interface RecommendationCallback {
        void onSuccess(String recommendation);
        void onFailure(String errorMessage);
    }
}
