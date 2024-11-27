package com.example.clothesme_android;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherClient {
    private static final String API_KEY = "e0dcd689763d6f1fa4290fc39d92c971";
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

    // 도시 이름을 한국어로 가져오는 메서드
//    public void getCityNameInKorean(double lat, double lon, CityNameCallback callback) {
//        ApiService apiService = RetrofitClient.getClient(BASE_URL).create(ApiService.class);
//
//        // Reverse Geocoding API 호출
//        Call<List<GeoResponse>> call = apiService.getReverseGeo(lat, lon, 1, "kr", API_KEY);
//
//        call.enqueue(new Callback<List<GeoResponse>>() {
//            @Override
//            public void onResponse(Call<List<GeoResponse>> call, Response<List<GeoResponse>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    String cityNameInKorean = response.body().get(0).getLocalNames().get("kr");
//                    callback.onCityNameReceived(cityNameInKorean != null ? cityNameInKorean : "도시 이름 없음");
//                } else {
//                    callback.onFailure("도시 이름을 가져오는 데 실패했습니다.");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<List<GeoResponse>> call, Throwable t) {
//                callback.onFailure("요청 실패: " + t.getMessage());
//            }
//        });
//    }

    // 도시 이름을 반환하는 콜백 인터페이스
//    public interface CityNameCallback {
//        void onCityNameReceived(String cityNameInKorean);
//        void onFailure(String errorMessage);
//    }

    // 날씨 데이터 받았을 때 콜백 인터페이스
    public interface WeatherDataCallback {
        void onWeatherDataReceived(WeatherResponse weatherResponse);
        void onFailure(String errorMessage);
    }
}
