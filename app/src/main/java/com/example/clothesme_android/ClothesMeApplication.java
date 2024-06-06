package com.example.clothesme_android;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClothesMeApplication extends AppCompatActivity {
    private ViewPager2 viewPager2;
    private TextToSpeech textToSpeech;
    private final Handler sliderHandler = new Handler();
    private boolean isFirstTime = true;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager2 = findViewById(R.id.image_slider);

        List<SliderItems> sliderItems = new ArrayList<>();
        sliderItems.add(new SliderItems(R.drawable.function1)); //옷 분석 기능
        sliderItems.add(new SliderItems(R.drawable.function2)); //양말 짝 판별 기능
        sliderItems.add(new SliderItems(R.drawable.function3)); //날씨에 맞는 옷 추천 기능

        viewPager2.setAdapter(new SliderAdapter(sliderItems, viewPager2, this));

        viewPager2.setClipToPadding(false);
        viewPager2.setClipChildren(false);
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        compositePageTransformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
            page.setAlpha(0.5f + (1 - Math.abs(position)));
        });

        viewPager2.setPageTransformer(compositePageTransformer);

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (!isFirstTime) {
                    speakImageDescription(position);
                } else {
                    isFirstTime = false;
                }
            }
        });

        // TTS
        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.KOREAN); // 언어 설정 (한국어)
            }
        });

        viewPager2.setOnClickListener(v -> {
            int position = viewPager2.getCurrentItem();
        });

        // 하단 흰색 버튼 클릭 이벤트
        findViewById(R.id.white_btn).setOnClickListener(v -> replayIntroduction());
    }

    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            viewPager2.setCurrentItem(viewPager2.getCurrentItem() + 1);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // 이미지에 대한 설명을 음성으로 출력하는 메서드
    public void speakImageDescription(int position) {
        int realPosition = position % 3;
        String description;
        switch (realPosition) {
            case 0:
                description = "옷 종류, 패턴, 색상 분석";
                break;
            case 1:
                description = "양말 짝 판별";
                break;
            case 2:
                description = "날씨에 맞는 옷 추천";
                break;
            default:
                description = "하단의 흰색 버튼을 누르면,  클로즈미 어플 사용 방법이 음성으로 안내됩니다.";
        }
        textToSpeech.speak(description, TextToSpeech.QUEUE_FLUSH, null, "ImageDescription");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TextToSpeech 리소스 해제
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    // 설명 다시 듣기 (하단 흰 버튼)
    private void replayIntroduction() {
        String description = "클로즈미 어플 사용 방법을 안내해드리겠습니다."+ "화면 중앙에 있는 초록 버튼을 양 옆으로 슬라이드하면, 해당 기능에 대한 설명이 음성으로 안내됩니다." +
                "초록 버튼을 한번 누르면, 기능에 대한 설명이 음성으로 한번 더 안내되며, 두번 연속으로 누르면, 해당 기능이 실행됩니다. " +
                "어플 사용 방법을 다시 듣고싶으시다면, 하단의 흰색 버튼을 눌러주세요.";
        textToSpeech.speak(description, TextToSpeech.QUEUE_FLUSH, null, "replayIntroduction");
    }

    public void stopAllTTS() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }
}
