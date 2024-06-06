package com.example.clothesme_android;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.clothesme_android.databinding.ActivityCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

public class CameraActivity extends AppCompatActivity {
    private ActivityCameraBinding viewBinding;
    private ImageCapture imageCapture = null;
    private ExecutorService cameraExecutor;
    private TextToSpeech textToSpeech;
    private String requestType;

    private static final String TAG = "CameraXApp";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ?
            new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE} :
            new String[]{Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        // 인텐트에서 REQUEST_TYPE 받아오기
        Intent intent = getIntent();
        requestType = intent.getStringExtra("REQUEST_TYPE");

        // 카메라 사용 권한
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        viewBinding.imageCaptureButton.setOnClickListener(v -> takePhoto());
        cameraExecutor = Executors.newSingleThreadExecutor();

        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.KOREAN); // 언어 설정 (한국어)
                // 설명을 음성으로 출력
                String description = "카메라가 실행되었습니다."+"하단의 흰색 버튼을 누르면 촬영됩니다.";
                textToSpeech.speak(description, TextToSpeech.QUEUE_FLUSH, null, "camera");
            }
        });
    }

    private void takePhoto() {
        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) {
            return;
        }

        // MediaStore의 표시 이름이 고유하도록 현재 시간을 기준으로 타임스탬프를 사용
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.KOREAN)
                .format(new Date());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        // Android Q 이상에서는 RELATIVE_PATH를 사용하여 저장 경로 지정
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        // 이미지 저장 위치와 메타데이터 지정
        // 이 객체에서 원하는 출력 방법을 지정
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        try {
                            // 비트맵 파일을 생성하여 저장하고 서버로 업로드
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), output.getSavedUri());
                            File imageFile = saveBitmapToJpeg(bitmap, getApplicationContext());
                            uploadImageFile(imageFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
        );
    }

    private void uploadImageFile(File imageFile) {
        RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("multipart/form-data"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", imageFile.getName(), fileBody);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
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
                        Toast.makeText(CameraActivity.this, "이미지 파일 업로드에 성공했습니다", Toast.LENGTH_SHORT).show();
                    } else { // 업로드에 실패했을 경우
                        Toast.makeText(CameraActivity.this, "이미지 파일 업로드에 실패했습니다", Toast.LENGTH_SHORT).show();
                        Log.e("Img Send Test fail message : ", jsonObject.getString("message"));
                    }
                } catch (JSONException e) { // 업로드에 실패했을 경우
                    Toast.makeText(CameraActivity.this, "이미지 파일 업로드에 실패했습니다", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(CameraActivity.this, "서버와 연결에 실패했습니다", Toast.LENGTH_SHORT).show();
                Log.e("Img Send Test fail message : ", t.getMessage());
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Preview preview = new Preview.Builder()
                    .build();
            preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());

            imageCapture = new ImageCapture.Builder().build();
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            try {
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);
            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
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