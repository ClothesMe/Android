package com.example.clothesme_android;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    String BASE_URL = "http://10.0.2.2:8000/";

    @Multipart
    @POST("/socks")
    Call<String> uploadImageForClothesAnalysis(@Part MultipartBody.Part imageFile);

    @Multipart
    @POST("/clothes")
    Call<String> uploadImageForSocksDetection(@Part MultipartBody.Part imageFile);

    // @Multipart
    // @POST("recommendation")
    // Call<ResponseBody> uploadImageForWeatherRecommendation(@Part MultipartBody.Part file);
}