package com.example.qrcode_videopacking.data;

import com.example.qrcode_videopacking.model.ResponseCheckBeforeRecord;
import com.example.qrcode_videopacking.model.ResponseUploadChunkFile;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface RestApi {

    @FormUrlEncoded
    @POST("packing/checkBeforeRecord")
    Call<ResponseCheckBeforeRecord> checkBeforeRecord(
            @Field("tracking_number") String tracking_number
    );

    @Multipart
    @POST("upload/chunk")
    Call<ResponseUploadChunkFile> uploadChunkFile(
            @Part MultipartBody.Part ax_file_chunk,
            @Part("filename") RequestBody ax_file_name,
            @Part("chunkIndex") RequestBody ax_chunk_index,
            @Part("totalChunks") RequestBody ax_total_chunks,
            @Part("fileIdentifier") RequestBody ax_file_identifier,
            @Part("fileExtension") RequestBody ax_file_extension,
            @Part("tracking_number") RequestBody ax_tracking_number
    );
}