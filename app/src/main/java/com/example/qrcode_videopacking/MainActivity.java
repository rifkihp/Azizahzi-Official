package com.example.qrcode_videopacking;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private final static int PERMISSION_CAMERA_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        replaceFragment(new QrScanFragment());

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.itemScan);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.itemCreate) {
                    bottomNavigationView.getMenu().findItem(R.id.itemCreate).setChecked(true);
                    replaceFragment(new QrCreateFragment());

                } else if (id == R.id.itemScan) {
                    //Intent intent = new Intent(getApplicationContext(), CaptureActivity.class);
                    //intent.putExtra("URL_TEXT", "JX123455444");
                    //startActivity(intent);

                    bottomNavigationView.getMenu().findItem(R.id.itemScan).setChecked(true);
                    replaceFragment(new VideoCaptureFragment());

                } else if (id == R.id.itemSettings) {
                    bottomNavigationView.getMenu().findItem(R.id.itemSettings).setChecked(true);
                    replaceFragment(new QrOptionsFragment());

                }
                return false;
            }
        });
    }

    @Override   // バックグラウンドから戻ったとき
    protected void onResume() {
        super.onResume();

        //  ■ 権限確認: カメラの権限をチェック
        checkPermission();

        // bottomNavigationView選択項目の初期化
        bottomNavigationView.getMenu().findItem(R.id.itemScan).setChecked(true);

        // QrScanFragmentを再読み込み (再スキャンのため）
        replaceFragment(new VideoCaptureFragment());
    }

    @Override   // バックグラウンドになったとき
    protected void onPause() {
        super.onPause();
    }

    // ■ 設定メソッド: Fragment(画面)の切り替え
    private void replaceFragment(Fragment fragment) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout,fragment);
        fragmentTransaction.commit();
    }

    // ■ 設定メソッド: カメラ利用権限の確認
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
                //メッセージを表示
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("アプリの利用にはカメラのアクセス権限が必要です。");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //承認要求
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CAMERA_REQUEST);
                    }
                });
                builder.setNegativeButton("拒否", null);
                builder.show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CAMERA_REQUEST);
            }
        }
    }
}