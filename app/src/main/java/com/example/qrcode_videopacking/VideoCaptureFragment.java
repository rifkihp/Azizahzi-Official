package com.example.qrcode_videopacking;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoCaptureFragment extends Fragment {

    View view;
    ExecutorService service;
    Recording recording = null;
    VideoCapture<Recorder> videoCapture = null;
    ImageButton capture, toggleFlash, flipCamera;
    PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    String qrcode = "";
    CountDownTimer stopRecordCountDownTimer;
    CountDownTimer currentRecordCountDownTimer;
    MediaPlayer mp_start;
    MediaPlayer mp_stop;
    MediaPlayer mp_tick;
    MediaPlayer mp_warn;

    boolean isRecord = false;
    boolean prosesStop = false;

    ActivityResultLauncher<String> activityResultLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_video_capture, container, false);

        previewView = view.findViewById(R.id.viewFinder);
        capture = view.findViewById(R.id.capture);
        toggleFlash = view.findViewById(R.id.toggleFlash);
        flipCamera = view.findViewById(R.id.flipCamera);

        mp_start = MediaPlayer.create(getActivity(), R.raw.start);
        mp_stop = MediaPlayer.create(getActivity(), R.raw.stop);
        mp_tick = MediaPlayer.create(getActivity(), R.raw.tick);
        mp_warn = MediaPlayer.create(getActivity(), R.raw.warn);

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera(cameraFacing);
            }
        });

        capture.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.CAMERA);
            } else if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO);
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                captureVideo();
            }
        });

        if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }

        flipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera(cameraFacing);
            }
        });

        service = Executors.newSingleThreadExecutor();

        stopRecordCountDownTimer = new CountDownTimer(1000, 1000) {
            public void onTick(long millisUntilFinished) {

            }
            public void onFinish() {
                Log.d("DUBIDU", "stop record");
                this.cancel();
                captureVideo();
            }
        };

        currentRecordCountDownTimer = new CountDownTimer(60000, 1000) { // 30 seconds, 1-second intervals
            public void onTick(long millisUntilFinished) {
                int detik = (int) millisUntilFinished / 1000;
                Log.i("TIME COUNTDOWN", detik + " seconds remaining to stop");
                if(detik==3) {
                    mp_warn.start();
                } else
                if (detik>3) {
                    mp_tick.start();
                }
            }

            public void onFinish() {
                Log.i("TIME COUNTDOWN", "Timer finished!");
                captureVideo();
            }
        };

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void captureVideo() {
        capture.setImageResource(R.drawable.round_stop_circle_24);
        Recording recording1 = recording;
        if (recording1 != null) {
            recording1.stop();
            recording = null;
            return;
        }

        isRecord = true;
        prosesStop = false;
        mp_start.start();
        currentRecordCountDownTimer.start();
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name+"-"+qrcode);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getActivity().getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues).build();

        if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        recording = videoCapture.getOutput().prepareRecording(getActivity().getApplicationContext(), options).withAudioEnabled().start(ContextCompat.getMainExecutor(getActivity().getApplicationContext()), videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                capture.setEnabled(true);
            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                    String msg = "Video capture succeeded: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                    Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

                } else {
                    recording.close();
                    recording = null;
                    String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
                    Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
                currentRecordCountDownTimer.cancel();
                mp_stop.start();

                capture.setImageResource(R.drawable.round_fiber_manual_record_24);
                isRecord = false;
                prosesStop = false;
            }
        });
    }

    private void replaceFragment(Fragment fragment) {

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout,fragment);
        fragmentTransaction.commit();

    }

    public void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> processCameraProvider = ProcessCameraProvider.getInstance(getActivity().getApplicationContext());

        processCameraProvider.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = processCameraProvider.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                //.setTargetResolution(new Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getActivity().getApplicationContext()), new QRCodeImageAnalyzer(new QRCodeFoundListener() {
                    @Override
                    public void onQRCodeFound(String _qrCode) {
                        if(_qrCode.equalsIgnoreCase("STOP")) {
                            if (isRecord && !prosesStop) {
                                prosesStop = true;
                                stopRecordCountDownTimer.start();
                            }

                            return;
                        }

                        if(!isRecord) {
                            qrcode = _qrCode;
                            captureVideo();
                        }




                    }

                    @Override
                    public void qrCodeNotFound() {
                        //qrCodeFoundButton.setVisibility(View.INVISIBLE);
                    }
                }));

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.LOWEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                cameraProvider.unbindAll();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                Camera camera = cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, imageAnalysis, preview, videoCapture);
                toggleFlash.setOnClickListener(view -> toggleFlash(camera));

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();

            }
        }, ContextCompat.getMainExecutor(getActivity().getApplicationContext()));
    }

    private void toggleFlash(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.round_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.round_flash_on_24);
            }
        } else {
            getActivity().runOnUiThread(() -> Toast.makeText(getActivity().getApplicationContext(), "Flash is not available currently", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

    }

}

