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
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.example.qrcode_videopacking.libs.DatabaseHandler;
import com.example.qrcode_videopacking.libs.GalleryFilePath;
import com.example.qrcode_videopacking.model.ResponseProcessUpload;
import com.example.qrcode_videopacking.model.fileUpload;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
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

    DatabaseHandler dh;

    //UPLOAD FILE

    int count_of_items = 1;
    Uri[] mFileCapture = new Uri[count_of_items];
    Handler[] mHandlerUpload = new Handler[count_of_items];

    ProgressBar pbVideoUpload;
    TextView stVideoUpload;

    CountDownTimer waitToStartUpload;

    Chronometer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_capture);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        /*if (Build.VERSION.SDK_INT >= 23) {
            insertDummyContactWrapper();
        }*/


        timer              = findViewById(R.id.simpleChronometer);
        pbVideoUpload = findViewById(R.id.pbVideoUpload);
        stVideoUpload = findViewById(R.id.stVideoUpload);

        timer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
                int seconds = (int) (elapsedMillis / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                seconds = seconds % 60;
                minutes = minutes % 60;

                String formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                chronometer.setText(formattedTime);
            }
        });

        pbVideoUpload.setVisibility(View.GONE);
        stVideoUpload.setVisibility(View.GONE);
        for(int i=0; i<count_of_items; i++) {
            mFileCapture[i] = null;
            mHandlerUpload[i] = new Handler();
        }

        context = VideoCaptureActivity.this;
        dh = new DatabaseHandler(context);
        dh.createTable();

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

        waitToStopCountDownTimer = new CountDownTimer(500, 500) { // 30 seconds, 1-second intervals
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                waitToStart = true;
                prosesStop = true;
                captureVideo();
            }
        };

        waitToStartUpload = new CountDownTimer(1000, 1000) { // 30 seconds, 1-second intervals
            public void onTick(long millisUntilFinished) {
                Log.e("CIMO", "CHECKPOINT CHECK UPLOAD ...");
                if(mFileCapture[0]==null) {
                    try {
                        //delete complete upload file
                        dh.deleteUploadData();

                        //get file upload:
                        ArrayList<String> data = dh.getUploadData();
                        for (String nama_file : data) {
                            Log.e("CIMO", "START UPLOAD FILE: " + nama_file);
                            initialFileUpload(0, nama_file);
                        }
                    } catch (Exception e) {
                        Log.e("CIMO", "Error: "+ e.getMessage());
                    }
                }
            }

            public void onFinish() {
                waitToStartUpload.start();
            }
        };
        
        waitToStartUpload.start();
    }

    private void startBackgroundTask() {
        // Create a new Thread
        Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Simulate a long-running background task


                // Update the UI on the main thread using runOnUiThread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultTextView.setText(result);
                    }
                });
            }
        });

        // Start the background thread
        backgroundThread.start();
    }
    
    private void resetView() {
        String formattedTime = String.format("%02d:%02d:%02d", 0, 0, 0);
        timer.setText(formattedTime);
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
        video_packing = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(System.currentTimeMillis()) + "_" + qrcode;
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
                    Uri mFileCaptured = ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();


                    Toast.makeText(this, mFileCaptured.toString(), Toast.LENGTH_SHORT).show();
                    dh.inserUploadtData(mFileCaptured.toString());

                    /*for(int i=0; i<count_of_items; i++) {
                        if (mFileCapture[i]==null) {
                            initialFileUpload(i, mFileCaptured.toString());
                            break;
                        }
                    }*/

                    /*RequestBody noresi       = RequestBody.create(MediaType.parse("text/plain"), qrcode);
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
                    });*/


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

    void initialFileUpload(int index_of_items, String source) {
        pbVideoUpload.setProgress(0);
        pbVideoUpload.setVisibility(View.VISIBLE);
        stVideoUpload.setVisibility(View.VISIBLE);

        mFileCapture[index_of_items]   = Uri.parse(source);
        mHandlerUpload[index_of_items] = new Handler();

        try {
            File file  = new File(mFileCapture[index_of_items]!=null?GalleryFilePath.getPath(context, mFileCapture[index_of_items]):"");
            String ext = getFileExtension(file.getName());
            String des = "./uploads/video_packing";
            uploadChuckFile_(file, des, file.getName(), ext, 0, index_of_items);
        } catch (IOException e) {
            stVideoUpload.setText(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void uploadChuckFile_(File file, String destination, String filename, String ext, int start, int position) throws IOException {

        mHandlerUpload[position].post(new Runnable() {
            @Override
            public void run() {
                mHandlerUpload[position].removeCallbacks(this);
                try {
                    uploadChuckFile(file, destination, filename, ext, start, position);
                } catch (IOException e) {

                    stVideoUpload.setText(e.getMessage());
                    e.printStackTrace();
                }
            }
        });

    }

    public void uploadChuckFile(File file, String destination, String filename, String ext, int start, int position) throws IOException {
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
        Call<ResponseProcessUpload> uploadDokumenCall = api.uploadVideoPacking(
                ax_file_input,
                ax_file_path,
                ax_allow_ext,
                ax_file_name,
                ax_max_file_size,
                ax_start_byte,
                ax_last_chunk
        );

        uploadDokumenCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseProcessUpload> call, @NonNull Response<ResponseProcessUpload> response) {
                int status = response.body().getStatus();
                String info = response.body().getInfo();
                String name = response.body().getName();

                double i = 100 * ((double) end/(double) size);
                int persen = (int) i;

                if (status == -1) {
                    //error
                    Log.e("CIMO", "error file upload: " + info + " % ");
                    pbVideoUpload.setVisibility(View.GONE);
                    stVideoUpload.setVisibility(View.VISIBLE);
                    stVideoUpload.setText(info);
                } else if (end == size) {
                    //DONE
                    dh.updateUploadData(mFileCapture[position].toString());
                    pbVideoUpload.setVisibility(View.GONE);
                    stVideoUpload.setVisibility(View.GONE);
                    mFileCapture[position] = null;
                    mHandlerUpload[position] = new Handler();
                } else {
                    Log.e("CIMO", "file upload: " + persen + " % ");
                    pbVideoUpload.setProgress(persen);
                    stVideoUpload.setText(persen + " % ");
                    try {
                        uploadChuckFile_(file, destination, filename, ext, end, position);
                    } catch (IOException e) {
                        Log.e("CIMO", "error file upload: " + e.getMessage());
                        pbVideoUpload.setVisibility(View.GONE);
                        stVideoUpload.setVisibility(View.VISIBLE);
                        stVideoUpload.setText(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseProcessUpload> call, @NonNull Throwable e) {
                Log.e("CIMO", "error file upload: " + e.getMessage());
                pbVideoUpload.setVisibility(View.GONE);
                stVideoUpload.setVisibility(View.VISIBLE);
                stVideoUpload.setText(e.getMessage());
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

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');

        // Handle cases with no extension or where the dot is the last character
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        } else {
            return fileName.substring(dotIndex + 1);
        }
    }

    private void insertDummyContactWrapper() {
        List<String> permissionsNeeded = new ArrayList<>();
        final List<String> permissionsList = new ArrayList<>();

        if (!addPermission(permissionsList, android.Manifest.permission.INTERNET))
            permissionsNeeded.add("INTERNET");
        if (!addPermission(permissionsList, android.Manifest.permission.ACCESS_NETWORK_STATE))
            permissionsNeeded.add("ACCESS_NETWORK_STATE");
        if (!addPermission(permissionsList, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionsNeeded.add("WRITE_EXTERNAL_STORAGE");
        if (!addPermission(permissionsList, android.Manifest.permission.READ_EXTERNAL_STORAGE))
            permissionsNeeded.add("READ_EXTERNAL_STORAGE");
        if (!addPermission(permissionsList, android.Manifest.permission.CAMERA))
            permissionsNeeded.add("CAMERA");
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_MEDIA_LOCATION))
            permissionsNeeded.add("ACCESS_MEDIA_LOCATION");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);

                //showMessageOKCancel(message, new DialogInterface.OnClickListener() {
                //@Override
                //public void onClick(DialogInterface dialog, int which) {*/
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                }
                //}
                //});
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            }
            return;
        }
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
                // Check for Rationale Option
                if (!shouldShowRequestPermissionRationale(permission))
                    return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(android.Manifest.permission.INTERNET, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.ACCESS_NETWORK_STATE, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.ACCESS_MEDIA_LOCATION, PackageManager.PERMISSION_GRANTED);

                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
                        && perms.get(android.Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted

                    startCamera(cameraFacing);
                    //Toast.makeText(context, "You Okay?", Toast.LENGTH_SHORT).show();

                } else {
                    // Permission Denied
                    Toast.makeText(context, "Some Permission is Denied", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}