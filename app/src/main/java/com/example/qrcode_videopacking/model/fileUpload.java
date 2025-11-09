package com.example.qrcode_videopacking.model;

public class fileUpload {

    boolean status;
    String message;

    public fileUpload(boolean status, String message){
        this.status = status;
        this.message = message;
    }

    public void setStatus(boolean status) {
        this.status = status;
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
