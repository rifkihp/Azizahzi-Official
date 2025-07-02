package com.example.qrcode_videopacking;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class QrCreateFragment extends Fragment {

    View view;
    ImageView qrImageView_qfc;
    TextView urlTextView_qfc;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_qr_create, container, false);

        // ■ ボタン処理: URLからQRコード生成
        qrImageView_qfc = view.findViewById(R.id.qrImageView_qcf);
        urlTextView_qfc = view.findViewById(R.id.urlEditText_qcf);
        Button createButton_qcf = view.findViewById(R.id.createButton_qcf);
        createButton_qcf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // urlEditText_qfcに文字が入力されているか判定
                if (urlTextView_qfc.length() > 0) {
                    MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

                    // 文字が入っている場合（画像生成）
                    try {
                        BitMatrix bitMatrix = multiFormatWriter.encode(urlTextView_qfc.getText().toString(), BarcodeFormat.QR_CODE,300,300);
                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        Bitmap bitmapCreate = barcodeEncoder.createBitmap(bitMatrix);

                        qrImageView_qfc.setImageBitmap(bitmapCreate);

                        // 例外処理
                    }catch(WriterException e){
                        throw new RuntimeException(e);
                    }

                    // 文字が入っていない場合
                }else{
                    Toast.makeText(getActivity(),"テキストを入力してください。",Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ■ ボタン処理: QR画像保存
        ImageButton saveImageButton_qcf = view.findViewById(R.id.saveImageButton_qcf);
        saveImageButton_qcf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // saveImageView_qfc に画像が生成されているかの判定
                try{
                    Bitmap bitmap = ((BitmapDrawable) qrImageView_qfc.getDrawable()).getBitmap();

                    // 生成されている場合
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) qrImageView_qfc.getDrawable();
                    Bitmap bitmapSave = bitmapDrawable.getBitmap();

                    FileOutputStream fileOutputStream = null;
                    File sdCard = Environment.getExternalStorageDirectory();
                    File Directory = new File(sdCard.getAbsolutePath() + "/Download");
                    Directory.mkdir();

                    @SuppressLint("DefaultLocale") String filename = String.format("%d.jpg",System.currentTimeMillis());
                    File outfile = new File(Directory,filename);

                    // 画像生成
                    try {
                        fileOutputStream = new FileOutputStream(outfile);
                        bitmapSave.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();

                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.fromFile(outfile));
                        getActivity().sendBroadcast(intent);

                        Toast.makeText(getActivity(),"画像を保存しました。",Toast.LENGTH_SHORT).show();

                        // 例外処理
                    } catch(IOException e){
                        e.printStackTrace();
                        Toast.makeText(getActivity(),"画像の保存に失敗しました。",Toast.LENGTH_SHORT).show();
                    }
                }

                // 生成されていない場合
                catch(NullPointerException e){
                    e.printStackTrace();
                    Toast.makeText(getActivity(),"画像が存在しません。",Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ■ ボタン処理: テキストコピー
        ImageButton copyImageButton_qcf = view.findViewById(R.id.copyImageButton_qcf);
        copyImageButton_qcf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (urlTextView_qfc.length() > 0) {

                    ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("GET_TEXT", urlTextView_qfc.getText().toString());
                    if(clipboardManager == null) {

                        Toast.makeText(getActivity(),"テキストのコピーに失敗しました。",Toast.LENGTH_SHORT).show();
                    }else{
                        clipboardManager.setPrimaryClip(clip);
                        clip.getDescription();

                        Toast.makeText(getActivity(),"テキストをコピーしました。",Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(getActivity(),"テキストを入力してください。",Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ■ 処理: ボタン URLを開く
        ImageButton openImageBtn_qcf = view.findViewById(R.id.openImageButton_qcf);
        openImageBtn_qcf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Uri uri = Uri.parse(urlTextView_qfc.getText().toString());
                    Intent iUrl = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(iUrl);
                } catch (Exception e) {
                    Toast.makeText(getActivity(), "URLの認識に失敗しました。", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;    // onCreateView最終
    }
}