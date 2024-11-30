package com.example.clothesme_android;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final int CENTER_THRESHOLD = 50;

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
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

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
                        Log.i(TAG, "Photo saved: " + output.getSavedUri().toString());
                        detectObjects(new File(output.getSavedUri().getPath()));

                        Intent loadingIntent = new Intent(CameraActivity.this, LoadingActivity.class);
                        loadingIntent.putExtra("photoUri", output.getSavedUri().toString());
                        loadingIntent.putExtra("REQUEST_TYPE", requestType);
                        startActivity(loadingIntent);
                    }
                }
        );
    }

    private void detectObjects(File imageFile) {
        // OpenCV 및 YOLOv4 모델 로드
        Mat imageMat = Imgcodecs.imread(imageFile.getAbsolutePath());
        // YOLO 모델 설정
        Net net = Dnn.readNetFromDarknet("yolo.cfg", "yolo.weights");

        // 이미지 전처리
        Mat blob = Dnn.blobFromImage(imageMat, 1/255.0, new Size(416, 416), new Scalar(0, 0, 0), true, false);
        net.setInput(blob);

        // 객체 탐지
        List<Mat> outputLayers = new ArrayList<>();
        net.forward(outputLayers, net.getUnconnectedOutLayersNames());

        // 탐지된 객체의 좌표 확인
        for (Mat output : outputLayers) {
            for (int i = 0; i < output.rows(); i++) {
                double confidence = output.get(i, 5)[0];
                if (confidence > 0.5) { // 신뢰도 기준
                    // 객체의 좌표 가져오기
                    int centerX = (int) (output.get(i, 0)[0] * imageMat.cols());
                    int centerY = (int) (output.get(i, 1)[0] * imageMat.rows());

                    // 중앙값과 비교
                    if (Math.abs(centerX - (imageMat.cols() / 2)) > CENTER_THRESHOLD ||
                            Math.abs(centerY - (imageMat.rows() / 2)) > CENTER_THRESHOLD) {
                        String message = "객체가 중앙에서 벗어났습니다.";
                        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "objectPosition");
                    }
                }
            }
        }
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
}