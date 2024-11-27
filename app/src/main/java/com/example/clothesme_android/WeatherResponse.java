package com.example.clothesme_android;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("name") // 도시 이름
    private String name;

    @SerializedName("weather") // 날씨 상태
    private Weather[] weather;

    @SerializedName("main") // 온도, 습도 정보
    private Main main;

    public String getName() {
        return name;
    }

    public Weather[] getWeather() {
        return weather;
    }

    public Main getMain() {
        return main;
    }

    // 날씨 설명을 받아오기 위한 메소드
    public String getWeatherDescription() {
        if (weather != null && weather.length > 0) {
            return weather[0].getDescription(); // Weather 객체에서 description 가져오기
        } else {
            return "날씨 정보를 받을 수 없습니다.";
        }
    }

    // 온도
    public double getTemp() {
        return main.temp;
    }

    // 습도
    public int getHumidity() {
        return main.humidity;
    }

    // 최저 온도
    public double getMinTemp() {
        return main.temp_min;
    }

    // 최고 온도
    public double getMaxTemp() {
        return main.temp_max;
    }

    // Weather 내부 클래스 (날씨 상태)
    public static class Weather {
        @SerializedName("description") // 날씨 설명 (예: 흐리고 비/눈)
        private String description;

        public String getDescription() {
            return description;
        }
    }

    // Main 내부 클래스 (온도, 습도 정보)
    public static class Main {
        @SerializedName("temp")
        private double temp;

        @SerializedName("temp_min")
        private double temp_min;

        @SerializedName("temp_max")
        private double temp_max;

        @SerializedName("humidity")
        private int humidity;

        public double getTemp() {
            return temp;
        }

        public double getTemp_min() {
            return temp_min;
        }

        public double getTemp_max() {
            return temp_max;
        }

        public int getHumidity() {
            return humidity;
        }
    }
}
