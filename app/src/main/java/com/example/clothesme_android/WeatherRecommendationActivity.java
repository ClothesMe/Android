package com.example.clothesme_android;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import android.Manifest;

import java.util.Locale;

public class WeatherRecommendationActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocation, tvWeather, tvTemperature, tvRecommendation, tvHumidity, tvMinTemp, tvMaxTemp;
    private ImageView ivWeatherIcon;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initializeViews();

        // 위치 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 이미 허용되었으면 날씨와 추천을 가져옴
            fetchWeatherAndRecommendation();
        }

        // 홈 화면으로 이동 및 사용법 안내
        findViewById(R.id.image_retelling).setOnClickListener(v -> replayIntroduction());
        findViewById(R.id.white_btn).setOnClickListener(v -> {
            Intent intent = new Intent(WeatherRecommendationActivity.this, ClothesMeApplication.class);
            startActivity(intent);
            finish();
        });

        // 음성 안내
        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.KOREAN); // 언어 설정 (한국어)
                // speakResponseResult(); // 서버 응답 result 값 음성 출력
            }
        });
    }

    private void initializeViews() {
        tvLocation = findViewById(R.id.text_location);
        tvWeather = findViewById(R.id.text_weather);
        tvTemperature = findViewById(R.id.text_temperature);
        tvRecommendation = findViewById(R.id.text_recommendation);
        tvHumidity = findViewById(R.id.text_humidity);
        tvMinTemp = findViewById(R.id.text_min_temp);
        tvMaxTemp = findViewById(R.id.text_max_temp);
        ivWeatherIcon = findViewById(R.id.image_weather_icon);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(3000000) // 5분 간격
                    .setFastestInterval(60000);

            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        Toast.makeText(WeatherRecommendationActivity.this, "위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        Log.d("LocationTest", "Latitude: " + latitude + ", Longitude: " + longitude);

                        // 날씨 데이터를 가져오기
                        WeatherClient weatherClient = new WeatherClient();
                        weatherClient.fetchWeatherData(latitude, longitude, new WeatherClient.WeatherDataCallback() {
                            @Override
                            public void onWeatherDataReceived(WeatherResponse weatherResponse) {
                                updateWeatherUI(weatherResponse);
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(WeatherRecommendationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            };

            // 위치 업데이트 요청
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateWeatherUI(WeatherResponse weatherResponse) {
        // Activity가 파괴되지 않았는지 확인
        if (isFinishing() || isDestroyed()) {
            return; // Activity가 종료될 예정이라면 작업을 진행하지 않음
        }

        // 도시 이름
        String location = weatherResponse.getName();
        int weatherId = weatherResponse.getWeather()[0].getId();

        // 날씨 설명
        String weatherDescription = weatherResponse.getWeather()[0].getDescription(); // 배열의 첫 번째 요소 사용
        String iconCode = weatherResponse.getWeather()[0].getIcon();
        String iconUrl = "http://openweathermap.org/img/wn/" + iconCode + "@2x.png";

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

        // Glide 요청 전 Activity가 유효한지 확인 후 실행
        if (!isFinishing() && !isDestroyed()) {
            Glide.with(this).load(iconUrl).into(ivWeatherIcon);
        }

        // 날씨 정보를 기반으로 의상 추천 받기
        fetchRecommendation(weatherResponse);
    }

    private void fetchRecommendation(WeatherResponse weatherResponse) {
        // 날씨 정보를 기반으로 의상 추천을 받는 코드 (ChatGPT API 호출)
        ChatGPTClient chatGPTClient = new ChatGPTClient();
        chatGPTClient.getRecommendation(weatherResponse, new ChatGPTClient.RecommendationCallback() {
            @Override
            public void onSuccess(String recommendation) {
                // 성공적으로 응답을 받으면 의상 추천 결과를 화면에 표시

                tvRecommendation.setText(recommendation);
                // 추천 결과를 음성으로 출력
                if (textToSpeech != null) {
                    textToSpeech.speak(recommendation, TextToSpeech.QUEUE_FLUSH, null, "recommendationSpeech");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // 실패한 경우 오류 메시지를 표시
                tvRecommendation.setText(errorMessage);
            }
        });
    }

    // 설명 다시 듣기 (우측 상단 초록 버튼)
    private void replayIntroduction() {
        String description = "클로즈미 어플 사용 방법을 안내해드리겠습니다." +
                "화면 중앙을 한 번 클릭하면, 날씨 정보와 추천 코디가 음성으로 다시 안내되며, " +
                "하단의 흰색 버튼을 한 번 누르면 홈으로 돌아갑니다." +
                "사용 방법을 다시 듣고싶으시다면, 우측 상단의 초록색 원형 버튼을 눌러주세요.";
        textToSpeech.speak(description, TextToSpeech.QUEUE_FLUSH, null, "replayIntroduction");
    }

    @Override
    protected void onDestroy() {
        // TTS 객체가 null이 아니면 종료
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
