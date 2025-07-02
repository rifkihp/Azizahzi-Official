package com.example.qrcode_videopacking;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.List;

public class QrScanFragment extends Fragment {

    View view;

    private CompoundBarcodeView compoundBarcodeView;
    private CameraSettings settings;

    private boolean hasCameraFlash = false;
    private boolean flashOn = false;

    private static int  frontCameraState = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // ■ 必須: MainActivityに呼ばれたとき表示
        view = inflater.inflate(R.layout.fragment_qr_scan, container, false);

        // ■ QRコードリーダー設定＆起動
        barcodeViewSetting();
        barcodeViewStart();


        // ■ 設定: FlashLight
        hasCameraFlash = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        ImageButton flashImageButton = view.findViewById(R.id.flashImageButton);
        flashImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(hasCameraFlash) {
                    if(flashOn) {
                        flashOn = false;
                        flashImageButton.setImageResource(R.drawable.icon_flash_off);
                        compoundBarcodeView.setTorchOff();
                    }else{
                        flashOn = true;
                        flashImageButton.setImageResource(R.drawable.icon_flash_on);
                        compoundBarcodeView.setTorchOn();
                    }
                } else {
                    Toast.makeText(getActivity(),"フラッシュライトが有効ではありません。",Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ■ 設定: FrontCamera
        ImageButton changeImageButton = view.findViewById(R.id.changeImageButton);
        changeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(frontCameraState == 0) {
                    frontCameraState = 1;
                    replaceFragment(new QrScanFragment());
                }else{
                    frontCameraState = 0;
                    replaceFragment(new QrScanFragment());
                }
            }
        });

        return view;    // onCreateView末尾
    }

    @Override
    public void onResume() {
        super.onResume();
        compoundBarcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        compoundBarcodeView.resume();
    }

    // ■ 設定メソッド: QRコードリーダーを初期化し、起動
    private void barcodeViewSetting() {

        compoundBarcodeView = (CompoundBarcodeView) view.findViewById(R.id.compaundBarcodeView);
        settings = compoundBarcodeView.getBarcodeView().getCameraSettings();
        settings.setRequestedCameraId(frontCameraState);
        compoundBarcodeView.getBarcodeView().setCameraSettings(settings);
        compoundBarcodeView.setStatusText("");
        compoundBarcodeView.resume();
        System.out.println(frontCameraState);
    }

    // ■ 実行メソッド: バーコードで読み取ったデータの処理
    public void barcodeViewStart() {
        compoundBarcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult barcodeResult) {
                // QrReadActivityに値を送る
                //Intent intent = new Intent(getActivity().getApplicationContext(), CaptureActivity.class);
                //intent.putExtra("URL_TEXT", barcodeResult.getText());
                //startActivity(intent);

                replaceFragment(new VideoCaptureFragment());
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> list) {
                // 処理なし
            }
        });

    }

    // ■ 設定メソッド: FrontCamera の利用時に、Fragmentを再生成する
    private void replaceFragment(Fragment fragment) {

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout,fragment);
        fragmentTransaction.commit();

    }
}

