package com.example.clothesme_android;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherClient {
    private static final String API_KEY = BuildConfig.OPENWEATHER_API_KEY;
    private static final String UNITS = "metric"; // 섭씨 사용
    private static final String LANG = "kr";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";

    public void fetchWeatherData(double lat, double lon, WeatherDataCallback callback) {
        // RetrofitClient에서 API 서비스 호출
        ApiService apiService = RetrofitClient.getClient(BASE_URL).create(ApiService.class);

        // 날씨 정보 요청
        Call<WeatherResponse> call = apiService.getWeather(lat, lon, API_KEY, UNITS, LANG);
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 날씨 데이터가 성공적으로 응답됨
                    callback.onWeatherDataReceived(response.body());
                } else {
                    callback.onFailure("날씨 데이터 가져오기 실패: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                callback.onFailure("요청 실패: " + t.getMessage());
            }
        });
    }

    // 날씨 데이터 받았을 때 콜백 인터페이스
    public interface WeatherDataCallback {
        void onWeatherDataReceived(WeatherResponse weatherResponse);
        void onFailure(String errorMessage);
    }
}
