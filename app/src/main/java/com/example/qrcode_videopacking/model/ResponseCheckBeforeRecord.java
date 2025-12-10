package com.example.qrcode_videopacking.model;

import com.google.gson.annotations.SerializedName;

public class ResponseCheckBeforeRecord {


    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    public boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
