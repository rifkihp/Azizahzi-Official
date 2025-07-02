package com.example.qrcode_videopacking;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qrcode_videopacking.data.RestApi;
import com.example.qrcode_videopacking.data.RetroFit;
import com.example.qrcode_videopacking.model.ResponseOrderReturn;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrReadActivity extends AppCompatActivity {

    private ImageView qrImageView;
    private TextView urlTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_read);

        // ■ 処理: QRコード読込で取得したURLデータを読込み
        urlTextView = findViewById(R.id.urlTextView);
        Intent intent = getIntent();
        if(intent != null) {
            String str = intent.getStringExtra("URL_TEXT");
            urlTextView.setText(str);


        // ■ 処理: QRコード生成
        qrImageView = findViewById(R.id.qrImageView);
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

        try {

            BitMatrix bitMatrix = multiFormatWriter.encode(urlTextView.getText().toString(), BarcodeFormat.QR_CODE,300,300);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmapCreate = barcodeEncoder.createBitmap(bitMatrix);

            qrImageView.setImageBitmap(bitmapCreate);

            }catch(WriterException e){
                throw new RuntimeException(e);
            }


            // ■ 処理: ボタン QR画像保存
            ImageButton saveImageButton = findViewById(R.id.saveImageButton);
            saveImageButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    RestApi api = RetroFit.getInstanceRetrofit();
                    Call<ResponseOrderReturn> splashCall = api.updateReturn(urlTextView.getText().toString());
                    splashCall.enqueue(new Callback<ResponseOrderReturn>() {
                        @Override
                        public void onResponse(@NonNull Call<ResponseOrderReturn> call, @NonNull Response<ResponseOrderReturn> response) {
                            boolean success = Objects.requireNonNull(response.body()).getSuccess();
                            if(success) {
                                String message = Objects.requireNonNull(response.body()).getMessage();
                                Toast.makeText(QrReadActivity.this,message,Toast.LENGTH_SHORT).show();


                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<ResponseOrderReturn> call, @NonNull Throwable t) {

                            Toast.makeText(QrReadActivity.this,"GAGAL SIMPAN DATA!",Toast.LENGTH_SHORT).show();
                        }
                    });

                    /*BitmapDrawable bitmapDrawable = (BitmapDrawable) qrImageView.getDrawable();
                    Bitmap bitmapSave = bitmapDrawable.getBitmap();

                    FileOutputStream fileOutputStream = null;
                    File sdCard = Environment.getExternalStorageDirectory();
                    File Directory = new File(sdCard.getAbsolutePath() + "/Download");
                    Directory.mkdir();

                    String filename = String.format("%d.jpg",System.currentTimeMillis());
                    File outfile = new File(Directory,filename);

                    try {
                        fileOutputStream = new FileOutputStream(outfile);
                        bitmapSave.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();

                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.fromFile(outfile));
                        sendBroadcast(intent);

                        Toast.makeText(QrReadActivity.this,"画像を保存しました。",Toast.LENGTH_SHORT).show();

                    } catch(IOException e){
                        e.printStackTrace();
                        Toast.makeText(QrReadActivity.this,"画像を保存に失敗しました。",Toast.LENGTH_SHORT).show();
                    }*/


                }
            });
        }

        // ■ 処理: ボタン テキストコピー
        ImageButton copyImageButton = findViewById(R.id.copyImageButton);
        copyImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("GET_TEXT", urlTextView.getText().toString());
                if(clipboardManager == null) {
                    Toast.makeText(QrReadActivity.this,"テキストのコピーに失敗しました。",Toast.LENGTH_SHORT).show();
                }else{
                    clipboardManager.setPrimaryClip(clip);
                    clip.getDescription();

                    Toast.makeText(QrReadActivity.this,"テキストをコピーしました。",Toast.LENGTH_SHORT).show();
                }


            }
        });


        // ■ 処理: ボタン URLを開く
        ImageButton openImageButton = findViewById(R.id.openImageButton);
        openImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Uri uri = Uri.parse(urlTextView.getText().toString());
                    Intent iUrl = new Intent(Intent.ACTION_VIEW,uri);
                    startActivity(iUrl);
                }catch(Exception e){
                    Toast.makeText(QrReadActivity.this,"ブラウザの起動に失敗しました。",Toast.LENGTH_SHORT).show();
                }
            }
        });


        // ■ 処理: 戻るボタン
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }   // onCreate最終

}