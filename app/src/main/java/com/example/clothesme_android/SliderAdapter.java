package com.example.clothesme_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
        if (position == sliderItems.size()- 2){
            viewPager2.post(runnable);
        }
        holder.imageView.setOnClickListener(v -> {
            // 해당 이미지에 대한 설명을 음성으로 들려주는 메서드 호출
            ((ClothesMeApplication)context).speakImageDescription(position);
        });

        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 더블 클릭 시 카메라 액티비티 실행
                Intent intent = new Intent(context, CameraActivity.class);
                context.startActivity(intent);
                return super.onDoubleTap(e);
            }
        });
        holder.imageView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
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