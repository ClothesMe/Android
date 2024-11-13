package com.example.clothesme_android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class LoadingActivity extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    private static final String TAG = "LoadingActivity";
    private String requestType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        ImageView gifImageView = findViewById(R.id.gif_image);
        Glide.with(this).load(R.drawable.loading_splash).into(gifImageView);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.KOREAN);
                textToSpeech.speak("촬영된 사진을 분석 중이니 잠시만 기다려주세요.", TextToSpeech.QUEUE_FLUSH, null, "loading");

            }
        });

        Uri photoUri = Uri.parse(getIntent().getStringExtra("photoUri"));
        requestType = getIntent().getStringExtra("REQUEST_TYPE");

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
            File imageFile = saveBitmapToJpeg(bitmap, getApplicationContext());
            uploadImageFile(imageFile, requestType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadImageFile(File imageFile, String requestType) {
        RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("multipart/form-data"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", imageFile.getName(), fileBody);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiService.BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        ApiService api = retrofit.create(ApiService.class);
        Call<String> call;

        // REQUEST_TYPE에 따라 올바른 엔드포인트로 요청
        if ("clothes".equals(requestType)) {
            call = api.uploadImageForClothesAnalysis(filePart);
        } else if ("socks".equals(requestType)) {
            call = api.uploadImageForSocksDetection(filePart);
        } else {
            call = api.uploadImageForClothesAnalysis(filePart);
        }

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.i("Img Send Test response code : ", "" + response.code());
                Log.i("Img Send Test response message : ", "" + response.body());

                String jsonStr = response.body();
                try {
                    // JSONObject로 변환
                    JSONObject jsonObject = new JSONObject(jsonStr);

                    if (jsonObject.getString("status").equals("success")) { // 업로드에 성공했을 경우
                        Toast.makeText(LoadingActivity.this, "이미지 파일 업로드에 성공했습니다", Toast.LENGTH_SHORT).show();
                        startFunctionActivity(imageFile.getAbsolutePath(), jsonStr);
                    } else { // 업로드에 실패했을 경우
                        Toast.makeText(LoadingActivity.this, "이미지 파일 업로드에 실패했습니다", Toast.LENGTH_SHORT).show();
                        Log.e("Img Send Test fail message : ", jsonObject.getString("message"));
                    }
                } catch (JSONException e) { // 업로드에 실패했을 경우
                    Toast.makeText(LoadingActivity.this, "이미지 파일 업로드에 실패했습니다", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(LoadingActivity.this, "서버와 연결에 실패했습니다", Toast.LENGTH_SHORT).show();
//                Log.e("Img Send Test fail message : ", t.getMessage());
                Log.e("Img Send Test fail message : ", t.getMessage(), t);
            }
        });
    }

    private void startFunctionActivity(String imagePath, String responseMessage) {
        Intent intent = new Intent(LoadingActivity.this, FunctionActivity.class);
        intent.putExtra("IMAGE_PATH", imagePath);
        intent.putExtra("RESPONSE_MESSAGE", responseMessage);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    // 비트맵을 파일로 변환하는 메소드
    public File saveBitmapToJpeg(Bitmap bitmap, Context context) {
        //내부저장소 캐시 경로를 받아옵니다.
        File storage = context.getCacheDir();

        //저장할 파일 이름
        String fileName = System.currentTimeMillis() + ".jpg";

        //storage 에 파일 인스턴스를 생성합니다.
        File imgFile = new File(storage, fileName);

        try {
            // 자동으로 빈 파일을 생성합니다.
            imgFile.createNewFile();

            // 파일을 쓸 수 있는 스트림을 준비합니다.
            FileOutputStream out = new FileOutputStream(imgFile);

            // compress 함수를 사용해 스트림에 비트맵을 저장합니다.
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            // 스트림 사용후 닫아줍니다.
            out.close();

            return imgFile;

        } catch (FileNotFoundException e) {
            Log.e("MyTag","FileNotFoundException : " + e.getMessage());
        } catch (IOException e) {
            Log.e("MyTag","IOException : " + e.getMessage());
        }

        return imgFile;
    }
}