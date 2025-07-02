package com.example.qrcode_videopacking;

public class GridMenuModel {
    String Title ;
    int Icon;

    public GridMenuModel(String Title, int Icon){
        this.Title = Title;
        this.Icon = Icon;
    }

    public String getTitle(){
        return this.Title;
    }

    public int getIcon(){
        return this.Icon;
    }
}
