package com.example.clothesme_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {
    private final List<SliderItems> sliderItems;
    private final ViewPager2 viewPager2;
    private final Context context;
    private Handler handler = new Handler();
    private boolean isSingleTap = false;
    private boolean isDoubleTap = false;

    SliderAdapter(List<SliderItems> sliderItems, ViewPager2 viewPager2, Context context) {
        this.sliderItems = sliderItems;
        this.viewPager2 = viewPager2;
        this.context = context;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SliderViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.slide_item, parent, false
                ));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.setImage(sliderItems.get(position));
        if (position == sliderItems.size() - 2) {
            viewPager2.post(runnable);
        }

        holder.imageView.setOnClickListener(v -> {
            if (isSingleTap) {
                isDoubleTap = true;
                isSingleTap = false;
                ((ClothesMeApplication) context).stopAllTTS();
                if (position == 2) { // 기능 3에 해당하는 포지션 (예: 2번 슬라이드)
                    Intent intent = new Intent(context, WeatherRecommendationActivity.class);
                    context.startActivity(intent);
                } else {
                    // 더블 클릭 시 카메라 액티비티 실행
                    Intent intent = new Intent(context, CameraActivity.class);

                    // 요청 타입을 슬라이드 아이템에서 가져와 인텐트에 추가
                    String requestType = sliderItems.get(position).getRequestType();
                    intent.putExtra("REQUEST_TYPE", requestType);

                    context.startActivity(intent);
                }
            } else {
                isSingleTap = true;
                handler.postDelayed(() -> {
                    if (!isDoubleTap) {
                        // 단일 클릭 시 이미지 설명
                        ((ClothesMeApplication) context).speakImageDescription(position);
                    }
                    isSingleTap = false;
                    isDoubleTap = false;
                }, 300);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sliderItems.size();
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_slider);
        }
        void setImage(SliderItems sliderItems){
            imageView.setImageResource(sliderItems.getImage());
        }
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            sliderItems.addAll(sliderItems);
            notifyDataSetChanged();
        }
    };
}