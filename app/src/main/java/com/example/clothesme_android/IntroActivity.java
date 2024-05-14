package com.example.clothesme_android;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class IntroActivity extends AppCompatActivity {
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.KOREAN); // 언어 설정 (한국어)
                // 설명을 음성으로 출력
                String description = "클로즈미 어플이 실행되었습니다." + "화면 중앙에 있는, 초록 버튼을 양 옆으로 슬라이드하면, 해당 기능에 대한 설명이, 음성으로 안내됩니다. " +
                        "초록 버튼을 한번 누르면, 기능에 대한 설명이, 음성으로 한번 더 안내되며, 두번 연속으로 누르면, 해당 기능이 실행됩니다. " +
                        "어플 사용 방법을 다시 듣고싶으시다면, 하단의 흰색 버튼을 눌러주세요.";
                textToSpeech.speak(description, TextToSpeech.QUEUE_FLUSH, null, "intro");
            }
        });

        // TTS 재생이 끝나면 인트로 액티비티를 종료
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                Intent intent = new Intent(getApplicationContext(), ClothesMeApplication.class);
                startActivity(intent); //splash 실행 후 바로 main으로 넘어감
                finish(); // 인트로 액티비티 종료
            }

            @Override
            public void onError(String s) {
                textToSpeech.setLanguage(Locale.KOREAN);
                String description = "클로즈미 어플을 종류 후, 다시 시도해주세요.";
                textToSpeech.speak(description, TextToSpeech.QUEUE_FLUSH, null, "intro");
            }
        });
    }

    @Override
    protected void onPause(){
        super.onPause();
        finish();
    }

    @Override
    protected void onDestroy() {
        // TTS 객체가 null이 아니면 종료
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}