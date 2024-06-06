package com.example.clothesme_android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FunctionActivity extends AppCompatActivity {
    private ImageButton imageButton;

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
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 이미지 버튼이 클릭되었을 때 실행할 코드
            }
        });
    }
    // 이미지를 회전시키는 메서드
    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}