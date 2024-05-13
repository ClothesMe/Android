package com.example.clothesme_android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Intent intent = new Intent(getApplicationContext(), ClothesMeApplication.class);
            startActivity(intent); //splash 실행 후 바로 main으로 넘어감
            // 이전 키를 눌렀을 때 splash로 이동을 방지하기 위해 finish 처리
            finish();
        }, 3000); // 3초동안 splash 화면 유지
    }

    @Override
    protected void onPause(){
        super.onPause();
        finish();
    }
}
