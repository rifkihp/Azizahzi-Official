package com.example.qrcode_videopacking.data;

import com.example.qrcode_videopacking.model.ResponseSaveRecord;
import com.example.qrcode_videopacking.model.ResponseProcessUpload;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface RestApi {

    @Multipart
    @POST("order/updateOrderPacking")
    Call<ResponseSaveRecord> saveVideoPacking(
            @Part("tracking_number") RequestBody tracking_number,
            @Part("video_packing") RequestBody video_packing
    );

    @Multipart
    @POST("order/uploadVideoPacking")
    Call<ResponseProcessUpload> uploadVideoPacking(
            @Part MultipartBody.Part ax_file_input,
            @Part("ax-file-path") RequestBody ax_file_path,
            @Part("ax-allow-ext") RequestBody ax_allow_ext,
            @Part("ax-file-name") RequestBody ax_file_name,
            @Part("ax-max-file-size") RequestBody ax_max_file_size,
            @Part("ax-start-byte") RequestBody ax_start_byte,
            @Part("ax-last-chunk") RequestBody ax_last_chunk
    );



}