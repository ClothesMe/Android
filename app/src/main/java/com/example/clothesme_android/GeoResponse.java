package com.example.clothesme_android;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class GeoResponse {
    @SerializedName("local_names")
    private Map<String, String> localNames;

    public Map<String, String> getLocalNames() {
        return localNames;
    }

    public void setLocalNames(Map<String, String> localNames) {
        this.localNames = localNames;
    }
}
