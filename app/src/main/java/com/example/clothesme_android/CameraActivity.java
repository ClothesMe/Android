package com.example.clothesme_android;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.LifecycleCameraController;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.clothesme_android.databinding.ActivityCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        LifecycleCameraController cameraController = new LifecycleCameraController(this);

        // 카메라 사용 권한
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        viewBinding.imageCaptureButton.setOnClickListener(v -> takePhoto());
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhoto() {
        // ImageCapture UseCase에 대한 참조를 가져온다. 만약 초기화되지 않았다면 함수를 종료.
        // UseCase는 이미지 캡처가 설정되기 전에 사진 버튼을 탭하면 null 된다.
        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) {
            return;
        }

        // MediaStore 콘텐츠 값을 만든다.
        // MediaStore의 표시 이름이 고유하도록 현재 시간을 기준으로 타임스탬프를 사용한다.
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(new Date());

        // ContentValues를 사용하여 이미지에 대한 메타데이터 설정
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        // Android Q 이상에서는 RELATIVE_PATH를 사용하여 저장 경로 지정
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        // OutputFileOptions 객체를 생성.
        // 이미지 저장 옵션 설정. MediaStore를 통해 이미지 저장 위치와 메타데이터 지정
        // 이 객체에서 원하는 출력 방법을 지정할 수 있다.
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        // takePicture()를 호출한다. 이미지 캡처 및 저장
        // outputOptions, 실행자, 이미지가 저장될 때 콜백을 전달
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                    }

                    // 캡처에 실패하지 않으면 사진을 저장하고 완료되었다는 토스트 메시지를 표시한다.
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        String msg = "Photo capture succeeded: " + output.getSavedUri();
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);
                    }
                }
        );
    }

    private void startCamera() {
        // ProcessCameraProvider 인스턴스를 생성한다.
        // 카메라의 수명 주기를 수명 주기 소유자와 바인딩하는 데 사용된다.
        // CameraX가 수명 주기를 인식하므로 카메라를 열고 닫는 작업이 필요하지 않게 된다.
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // cameraProviderFuture에 리스너를 추가한다.
        // 첫 번째 인수에는 Runnable를 넣는다.
        // 두 번째는 기본 스레드에서 실행되는 Executor를 넣는다.
        cameraProviderFuture.addListener(() -> {
            // 카메라 수명 주기를 애플리케이션 프로세스 내의 LifecycleOwner에 바인딩한다.
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Preview 객체를 초기화 하고 뷰파인더에서 노출 영역 제공자를 가져온 다음 Preview에서 설정
            Preview preview = new Preview.Builder()
                    .build();
            preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());

            // imageCaputre 인스턴스를 빌드
            imageCapture = new ImageCapture.Builder().build();

            // 후면 카메라를 선택하는 객체를 생성
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            // cameraProvider에 바인딩된 항목이 없도록 한 다음
            // 위에서 생성한 객체들을 cameraProvider에 바인딩
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
    }

    private static final String TAG = "CameraXApp";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ?
            new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE} :
            new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

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

    private static class LuminosityAnalyzer implements ImageAnalysis.Analyzer {
        private final LumaListener listener;

        LuminosityAnalyzer(LumaListener listener) {
            this.listener = listener;
        }

        private byte[] toByteArray(ByteBuffer buffer) {
            buffer.rewind();    // Rewind the buffer to zero
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);   // Copy the buffer into a byte array
            return data; // Return the byte array
        }

        @Override
        public void analyze(ImageProxy image) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = toByteArray(buffer);
            List<Integer> pixels = new ArrayList<>();
            for (byte datum : data) {
                pixels.add(datum & 0xFF);
            }
            double luma = pixels.stream().mapToInt(Integer::intValue).average().orElse(0.0);

            listener.onLumaAvailable(luma);

            image.close();
        }
    }

    interface LumaListener {
        void onLumaAvailable(double luma);
    }
}