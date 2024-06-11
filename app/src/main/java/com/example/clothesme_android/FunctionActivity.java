package com.example.clothesme_android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class FunctionActivity extends AppCompatActivity {
    private ImageButton imageButton;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function);

        imageButton = findViewById(R.id.image_function);

        // CameraActivity에서 전달한 이미지 파일 경로를 받아옵니다.
        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("IMAGE_PATH");

        // 이미지를 가져와서 설정합니다.
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

        bitmap = rotateBitmap(bitmap, 90); // 90도 회전

        if (bitmap != null) {
            imageButton.setImageBitmap(bitmap);
        } else {
            // 이미지를 가져오지 못했을 경우 에러 메시지를 출력합니다.
            Toast.makeText(this, "이미지를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }

        // 이미지 버튼 클릭 시 이벤트 처리
        imageButton.setOnClickListener(v -> {
            // 이미지 버튼이 클릭되었을 때 실행할 코드
        });

        // TTS
        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.KOREAN); // 언어 설정 (한국어)
            }
        });

        // 하단 흰색 버튼 클릭 이벤트
        findViewById(R.id.image_retelling).setOnClickListener(v -> replayIntroduction());
    }
    // 이미지를 회전시키는 메서드
    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    // 설명 다시 듣기 (우측 상단 초록 버튼)
    private void replayIntroduction() {
        String description = "클로즈미 어플 사용 방법을 안내해드리겠습니다."+ "화면 중앙에 있는 사진을 한 번 클릭하면, 해당 사진에 대한 분석 결과가 음성으로 다시 안내되며, " +
                "두 번 누르면, 카메라가 실행되어 다시 촬영을 할 수 있습니다." + "하단의 흰색 버튼을 한 번 누르면 홈으로 돌아갑니다." +
                "사용 방법을 다시 듣고싶으시다면, 우측 상단의 초록색 원형 버튼을 눌러주세요.";
        textToSpeech.speak(description, TextToSpeech.QUEUE_FLUSH, null, "replayIntroduction");
    }
}