package com.example.qrcode_videopacking.model;
import com.google.gson.annotations.SerializedName;

public class ResponseUploadChunkFile {
    
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

}
