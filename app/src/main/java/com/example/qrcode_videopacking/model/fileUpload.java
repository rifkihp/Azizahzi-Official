package com.example.qrcode_videopacking.model;

public class fileUpload {

    boolean status;

    int persentase;
    String message;

    public fileUpload(boolean status, int persentase, String message){
        this.status = status;
        this.persentase = persentase;
        this.message = message;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setPersentase(int persentase) {
        this.persentase = persentase;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getStatus() {
        return this.status;
    }

    public String getMessage() {
        return this.message;
    }
}
