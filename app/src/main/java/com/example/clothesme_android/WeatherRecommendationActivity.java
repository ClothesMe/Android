package com.example.clothesme_android;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.Manifest;

public class WeatherRecommendationActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private TextView tvLocation, tvWeather, tvTemperature, tvRecommendation, tvHumidity, tvMinTemp, tvMaxTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 위치 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 이미 허용되었으면 날씨와 추천을 가져옴
            fetchWeatherAndRecommendation();
        }

        // View 연결
        tvLocation = findViewById(R.id.text_location);
        tvWeather = findViewById(R.id.text_weather);
        tvTemperature = findViewById(R.id.text_temperature);
        tvRecommendation = findViewById(R.id.text_recommendation);
        tvHumidity = findViewById(R.id.text_humidity);
        tvMinTemp = findViewById(R.id.text_min_temp);
        tvMaxTemp = findViewById(R.id.text_max_temp);

        // 날씨 데이터 가져오기
        fetchWeatherAndRecommendation();
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchWeatherAndRecommendation();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchWeatherAndRecommendation() {
        // 위치 권한이 부여된 상태에서 위치 정보 가져오기
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // 현재 위치의 위도, 경도 가져오기
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();

                                // 위치 정보를 바탕으로 날씨 데이터 가져오기
                                WeatherClient weatherClient = new WeatherClient();
                                weatherClient.fetchWeatherData(latitude, longitude, new WeatherClient.WeatherDataCallback() {
                                    @Override
                                    public void onWeatherDataReceived(WeatherResponse weatherResponse) {
                                        // 날씨 데이터를 UI에 표시
                                        updateWeatherUI(weatherResponse);
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        Toast.makeText(WeatherRecommendationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(WeatherRecommendationActivity.this, "위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(this, "위치 권한을 허용해 주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateWeatherUI(WeatherResponse weatherResponse) {
        // 도시 이름
        String location = weatherResponse.getName();

        // 날씨 설명
        String weatherDescription = weatherResponse.getWeather()[0].getDescription(); // 배열의 첫 번째 요소 사용

        // 온도, 최저/최고 온도, 습도 정보
        String temperature = (int) weatherResponse.getMain().getTemp() + "°C";
        String minTemp = "최저 " + (int)  weatherResponse.getMain().getTemp_min() + "°C";
        String maxTemp = "최고 " + (int)  weatherResponse.getMain().getTemp_max() + "°C";
        String humidity = "습도 " + weatherResponse.getMain().getHumidity() + "%";

        // UI 업데이트
        tvLocation.setText(location);
        tvWeather.setText(weatherDescription);
        tvTemperature.setText(temperature);
        tvMinTemp.setText(minTemp);
        tvMaxTemp.setText(maxTemp);
        tvHumidity.setText(humidity);

        // 날씨 정보를 기반으로 의상 추천 받기
        fetchRecommendation(weatherResponse);
    }


    private void fetchRecommendation(WeatherResponse weatherResponse) {
        // 날씨 정보를 기반으로 의상 추천을 받는 코드 (ChatGPT API 호출)
        ChatGPTClient chatGPTClient = new ChatGPTClient();
        chatGPTClient.getRecommendation(weatherResponse, new ChatGPTClient.RecommendationCallback() {
            @Override
            public void onSuccess(String recommendation) {
                // 의상 추천 결과 화면에 표시
                tvRecommendation.setText(recommendation);
            }

            @Override
            public void onFailure(String errorMessage) {
                // 오류 메시지 화면에 표시
                tvRecommendation.setText(errorMessage);
            }
        });
    }

}
