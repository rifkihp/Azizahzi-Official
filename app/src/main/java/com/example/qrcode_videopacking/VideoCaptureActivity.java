package com.example.qrcode_videopacking;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import com.example.qrcode_videopacking.data.RestApi;
import com.example.qrcode_videopacking.data.RetroFit;
import com.example.qrcode_videopacking.model.ResponseSaveRecord;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoCaptureActivity extends AppCompatActivity {
    ExecutorService service;
    Recording recording = null;
    VideoCapture<Recorder> videoCapture = null;
    ImageButton capture, toggleFlash, flipCamera;
    PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera(cameraFacing);
        }
    });
    String qrcode = "";
    String video_packing = "";
    CountDownTimer currentRecordCountDownTimer;
    CountDownTimer waitToStartCountDownTimer;
    CountDownTimer waitToStopCountDownTimer;
    MediaPlayer mp_start;
    MediaPlayer mp_stop;
    MediaPlayer mp_tick;
    MediaPlayer mp_warn;
    boolean isRecord = false;
    boolean prosesStop = false;
    long maxDuration = 600000;
    int detik = 0;

    boolean waitToStart = false;

    boolean waitToStop  = false;

    Context context;
    Dialog dialog_loading;


    //UPLOAD FILE

    int count_of_items = 1;
    int index_of_items = 0;
    Uri[] mFileCapture = new Uri[count_of_items];
    int[] status_upload = new int[count_of_items]; //0:on Proses, 1:success, -1: gagal;
    Handler[] mHandlerUpload = new Handler[count_of_items];

    String[] upload_titles = new String[] {"Video Packing"};
    String[] pathfile = new String[count_of_items];

    String[] namefile = new String[count_of_items];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        setContentView(R.layout.activity_video_capture);

        for(int i=0; i<count_of_items; i++) {
            mFileCapture[i] = null;
            status_upload[i] = 0;
            mHandlerUpload[i] = new Handler();
            pathfile[i] = "";
            namefile[i] = "";
        }

        context = VideoCaptureActivity.this;
        dialog_loading = new Dialog(context);
        dialog_loading.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog_loading.setCancelable(false);
        dialog_loading.setContentView(R.layout.loading_dialog);

        mp_start = MediaPlayer.create(this, R.raw.start);
        mp_stop = MediaPlayer.create(this, R.raw.stop);
        mp_tick = MediaPlayer.create(this, R.raw.tick);
        mp_warn = MediaPlayer.create(this, R.raw.warn);

        previewView = findViewById(R.id.viewFinder);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipCamera);
        capture.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.CAMERA);
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO);
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                waitToStart = false;
                captureVideo();
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

        currentRecordCountDownTimer = new CountDownTimer(maxDuration, 1000) { // 30 seconds, 1-second intervals
            public void onTick(long millisUntilFinished) {
                detik = (int) millisUntilFinished / 1000;
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

        waitToStartCountDownTimer = new CountDownTimer(3000, 1000) { // 30 seconds, 1-second intervals
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                waitToStart = false;
                waitToStop  = false;
            }
        };

        waitToStopCountDownTimer = new CountDownTimer(3000, 1000) { // 30 seconds, 1-second intervals
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                waitToStart = true;
                prosesStop = true;
                captureVideo();
            }
        };
    }

    public void openDialogLoading() {
        dialog_loading.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog_loading.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        video_packing = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        video_packing = qrcode + "_" +video_packing;
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, video_packing);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");
        
        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        recording = videoCapture.getOutput().prepareRecording(this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                capture.setEnabled(true);
            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                    String msg = "Video capture succeeded: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

                    RequestBody noresi       = RequestBody.create(MediaType.parse("text/plain"), qrcode);
                    RequestBody videopacking = RequestBody.create(MediaType.parse("text/plain"), video_packing+".mp4");

                    openDialogLoading();
                    RestApi api = RetroFit.getInstanceRetrofit();
                    Call<ResponseSaveRecord> saveVideoPackingCall = api.saveVideoPacking(noresi, videopacking);
                    saveVideoPackingCall.enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<ResponseSaveRecord> call, @NonNull Response<ResponseSaveRecord> response) {
                            dialog_loading.dismiss();
                            boolean success = Objects.requireNonNull(response.body()).getSuccess();
                            if(success) {
                                String message = Objects.requireNonNull(response.body()).getMessage();
                                Toast.makeText(VideoCaptureActivity.this,message,Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<ResponseSaveRecord> call, @NonNull Throwable t) {
                            dialog_loading.dismiss();
                            Toast.makeText(VideoCaptureActivity.this,"GAGAL SIMPAN DATA!",Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    recording.close();
                    recording = null;
                    String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
                waitToStartCountDownTimer.start();
                currentRecordCountDownTimer.cancel();
                mp_stop.start();

                capture.setImageResource(R.drawable.round_fiber_manual_record_24);
                isRecord = false;
                prosesStop = false;
                qrcode = "";
            }
        });
    }

    public void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> processCameraProvider = ProcessCameraProvider.getInstance(this);

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

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new QRCodeImageAnalyzer(new QRCodeFoundListener() {
                    @Override
                    public void onQRCodeFound(String _qrCode) {
                        int condition = (int) maxDuration/1000;
                        if(detik<condition-10 && _qrCode.equalsIgnoreCase(qrcode)) {
                            if(!waitToStop) {
                                if (isRecord && !prosesStop) {
                                    waitToStop = true;
                                    waitToStopCountDownTimer.start();
                                }
                            }

                            return;
                        }

                        if(!waitToStart && !isRecord) {
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                activityResultLauncher.launch(Manifest.permission.CAMERA);
                            } else if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO);
                            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            } else {
                                qrcode = _qrCode;
                                captureVideo();
                            }
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

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview, videoCapture);

                toggleFlash.setOnClickListener(view -> toggleFlash(camera));
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public void uploadChuckFile_(File file, String destination, String filename, String ext, int start, ProgressBar pgbar, int position) throws IOException {

        mHandlerUpload[position].post(new Runnable() {
            @Override
            public void run() {
                mHandlerUpload[position].removeCallbacks(this);
                try {
                    uploadChuckFile(file, destination, filename, ext, start, pgbar, position);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void uploadChuckFile(File file, String destination, String filename, String ext, int start, ProgressBar pgbar, int position) throws IOException {
        FileInputStream f = new FileInputStream(file.getAbsolutePath());

        final int bytes_per_chunk = 1*1024*1024; // == 1MB
        final int size = f.available(); // Size of original file
        final int end = (start + bytes_per_chunk) >= size ? size : (start + bytes_per_chunk);

        byte[] data = new byte[size];  // Size of original file
        byte[] subData = new byte[bytes_per_chunk];  // 4MB Sized Array

        f.read(data); // Read The Data
        subData = Arrays.copyOfRange(data, start, end);
        RequestBody requestFile = RequestBody.create(MediaType.parse("*/*"), subData);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part ax_file_input = MultipartBody.Part.createFormData("ax_file_input",
                filename, // filename, this is optional
                requestFile
        );

        RequestBody ax_file_path = RequestBody.create(MediaType.parse("text/plain"), destination);
        RequestBody ax_allow_ext = RequestBody.create(MediaType.parse("text/plain"), ext);
        RequestBody ax_file_name = RequestBody.create(MediaType.parse("text/plain"), filename);
        RequestBody ax_max_file_size = RequestBody.create(MediaType.parse("text/plain"), "10G");
        RequestBody ax_start_byte = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(end));
        RequestBody ax_last_chunk = RequestBody.create(MediaType.parse("text/plain"), end == size ? "true" : "false");

        RestApi api = RetroFit.getInstanceRetrofit();
        Call<ResponseUploadDokumen> uploadDokumenCall = api.upload(
                ax_file_input,
                ax_file_path,
                ax_allow_ext,
                ax_file_name,
                ax_max_file_size,
                ax_start_byte,
                ax_last_chunk
        );

        uploadDokumenCall.enqueue(new Callback<ResponseUploadDokumen>() {
            @Override
            public void onResponse(@NonNull Call<ResponseUploadDokumen> call, @NonNull Response<ResponseUploadDokumen> response) {
                int status = response.body().getStatus();
                String info = response.body().getInfo();
                String name = response.body().getName();

                double i = 100 * ((double) end/(double) size);
                int persen = (int) i;

                Log.e("PGBARBAR", i+" % ");
                pgbar.setProgress(persen);
                if (status == -1) {
                    //error
                    status_upload[position] = -1;
                    UploadDokumenUtamaFragment.btFile_uploads[position].setVisibility(View.VISIBLE);
                    UploadDokumenUtamaFragment.pbFile_uploads[position].setVisibility(View.GONE);
                    UploadDokumenUtamaFragment.tvStatus_uploads[position].setVisibility(View.VISIBLE);
                    UploadDokumenUtamaFragment.tvStatus_uploads[position].setText(info);
                } else if (end == size) {
                    //DONE
                    status_upload[position] = 1;
                    UploadDokumenUtamaFragment.btFile_uploads[position].setVisibility(View.VISIBLE);
                    UploadDokumenUtamaFragment.pbFile_uploads[position].setVisibility(View.GONE);
                    UploadDokumenUtamaFragment.etFile_uploads[position].setText(name);
                    namefile[position] = name;
                } else {
                    try {
                        uploadChuckFile_(file, destination, filename, ext, end, pgbar, position);
                    } catch (IOException e) {
                        e.printStackTrace();
                        status_upload[position] = -1;
                        UploadDokumenUtamaFragment.btFile_uploads[position].setVisibility(View.VISIBLE);
                        UploadDokumenUtamaFragment.pbFile_uploads[position].setVisibility(View.GONE);
                        UploadDokumenUtamaFragment.tvStatus_uploads[position].setVisibility(View.VISIBLE);
                        UploadDokumenUtamaFragment.tvStatus_uploads[position].setText(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseUploadDokumen> call, @NonNull Throwable t) {
                t.printStackTrace();

                status_upload[position] = -1;
                //error
                showInformationDialog("Proses upload Dokumen Gagal.", false);
            }
        });
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
            runOnUiThread(() -> Toast.makeText(this, "Flash is not available currently", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        service.shutdown();
    }
}