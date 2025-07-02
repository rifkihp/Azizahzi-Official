package com.example.qrcode_videopacking.data;

import com.example.qrcode_videopacking.model.ResponseOrderReturn;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RestApi {

    @GET("order/setOrderReturn")
    Call<ResponseOrderReturn> updateReturn(
            @Query("trackingId") String trackingId
    );
}