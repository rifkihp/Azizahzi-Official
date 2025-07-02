package com.example.qrcode_videopacking.model;
import com.google.gson.annotations.SerializedName;

public class ResponseUploadDokumen {

    @SerializedName("name")
    String name;

    @SerializedName("size")
    private int size;

    @SerializedName("status")
    private int status;

    @SerializedName("info")
    String info;

    @SerializedName("more")
    String more;

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public int getStatus() {
        return status;
    }

    public String getInfo() {
        return info;
    }

    public String getMore() {
        return more;
    }

}
