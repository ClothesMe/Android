package com.example.clothesme_android;

public class SliderItems {
    private final int image;
    private final String requestType;

    public SliderItems(int image, String requestType) {
        this.image = image;
        this.requestType = requestType;
    }
    public int getImage() {
        return image;
    }
    public String getRequestType() {return requestType;}
}
