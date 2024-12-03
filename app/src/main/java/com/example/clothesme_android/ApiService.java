package com.example.clothesme_android;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    String BASE_URL = "http://10.0.2.2:8000/";

    @Multipart
    @POST("/clothes")
    Call<String> uploadImageForClothesAnalysis(@Part MultipartBody.Part imageFile);

    @Multipart
    @POST("/socks")
    Call<String> uploadImageForSocksDetection(@Part MultipartBody.Part imageFile);

    @GET("weather")
    Call<WeatherResponse> getWeather(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );

    @POST("chat/completions")
    Call<ChatGPTResponse> getRecommendation(@Body ChatGPTRequest request);
}