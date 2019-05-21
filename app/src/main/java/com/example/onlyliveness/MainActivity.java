package com.example.onlyliveness;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.androidadvance.topsnackbar.TSnackbar;
import com.example.liveness.Clock;
import com.example.liveness.OnClockTickListener;
import com.wang.avi.AVLoadingIndicatorView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";


    private OkHttpClient okHttpClient = null;
    // Process child thread sent command to show server response text in activity main thread.
    private Handler displayRespTextHandler = null;
    private static final int COMMAND_DISPLAY_SERVER_RESPONSE = 1;
    private static final String KEY_SERVER_RESPONSE_OBJECT = "KEY_SERVER_RESPONSE_OBJECT";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8; cache-control=no-cache");


    private String[] fivePicture_filePath = {null, null, null, null, null, null, null};
    private String[] fivePicture_base64 = {null, null, null, null, null, null, null};

    //private String[] dotPosition = {"CENTER", "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT", "TOP_LEFT"};
    private String[] dotPosition4 = {"CENTER", "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"};
    private String[] dotPosition5 = {"CENTER", "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT", "TOP_LEFT"};
    private String[] dotPosition6 = {"CENTER", "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT", "TOP_LEFT", "TOP_RIGHT"};
    private int pictureTakenMode = 1;   // 0 = 4 pics, 1 = 5 pics, 2 = 6 pics

    private TSnackbar snackbar;
    private ConstraintLayout constraintLayout;
    private ImageView referenceFrame;
    private Button asyncPostBtn;
    private ImageView captureLed;
    private ImageButton btnCapture;
    private TextureView textureView;

    private ImageView centerDot;
    private ImageView topLeftDot;
    private ImageView topRightDot;
    private ImageView bottomLeftDot;
    private ImageView bottomRightDot;
    private ImageView middleLeftDot;
    private TextView countDownTextView;
    private TextView referenceDotInfo;
    private TextView livenessTitle;
    private TextView realResult;
    private TextView realResultTitle;
    private TextView errorCodeTextView;
    private TextView MsgTextView;
    private TextView ScoreTextView;
    private TextView statusBar;
    private AVLoadingIndicatorView avi;
    private ImageView internetStatusImage;
    private TextView chooseNumberTitle;
    private Spinner spinner;
    private IntentFilter filter;
    private Base64SignalReceiver receiver;
    private IntentFilter photoFilter;
    private PhotoTakenSignalReceiver photoReceiver;
    private static String SIGNAL_RECEIVER = "Ready to send request";
    private static String PHOTO_TAKEN_RECEIVER = "ImageTaken";


    private int count = 0;
    private boolean processDone = false;
    private int timing = 0;
    private int internetCheckTiming = 0;
    private boolean firstTimeToastSnackbar = true;
    private boolean duringFaceDetectionSequence = false;
    private int[] countDownSequence = {4, 3, 2, 1};
    private int countDownPointer = 0;
    private boolean countDownStartEnable = true;
    private boolean startSequence = false;


    private static final int REQUEST_CAMERA_PERMISSION = 200;
    /**
     * 0 forback camera
     * 1 for front camera
     * Initlity default camera is front camera
     */
    public static final int CAMERA_FRONT = 1;
    public static final int CAMERA_BACK = 0;
    public int cameraSelected = CAMERA_FRONT;
    private String cameraId = String.valueOf(cameraSelected);

    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        //This setting is for rear camera
        //ORIENTATIONS.append(Surface.ROTATION_0, 90);
        //ORIENTATIONS.append(Surface.ROTATION_90, 0);
        //ORIENTATIONS.append(Surface.ROTATION_180, 270);
        //ORIENTATIONS.append(Surface.ROTATION_270, 180);
        //This setting is for front camera
        ORIENTATIONS.append(Surface.ROTATION_0, 270);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 90);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private double CardEyeDistanceMin = 0.015;
    private double CardEyeDistanceMax = 0.2;
    private double EyeDistanceMin = 0.1;
    private double EyeDistanceMax = 1.0;

    //Save to file
    private File file;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuidler;
    private Size imageDimension;
    private ImageReader imageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindWidget();
        setTypeFace();
        setInitializeState();
        setAction();

    }

    private void setAction() {



        constraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // custom dialog
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.parameters);

                Window window = dialog.getWindow();
                WindowManager.LayoutParams wlp = window.getAttributes();

                wlp.gravity = Gravity.RIGHT;
                wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                window.setAttributes(wlp);


                Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
                final EditText CardEyeMinEditText = dialog.findViewById(R.id.cardeyemin_editText);
                final EditText CardEyeMaxEditText = dialog.findViewById(R.id.cardeyemax_editText);
                final EditText EyeMinEditText = dialog.findViewById(R.id.eyemin_editText);
                final EditText EyeMaxEditText= dialog.findViewById(R.id.eyemax_editText);

                CardEyeMaxEditText.setText(String.valueOf(CardEyeDistanceMax));
                CardEyeMinEditText.setText(String.valueOf(CardEyeDistanceMin));

                EyeMaxEditText.setText(String.valueOf(EyeDistanceMax));
                EyeMinEditText.setText(String.valueOf(EyeDistanceMin));
                // if button is clicked, close the custom dialog
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        CardEyeDistanceMax = Double.parseDouble(CardEyeMaxEditText.getText().toString());
                        CardEyeDistanceMin = Double.parseDouble(CardEyeMinEditText.getText().toString());
                        EyeDistanceMax = Double.parseDouble(EyeMaxEditText.getText().toString());
                        EyeDistanceMin = Double.parseDouble(EyeMinEditText.getText().toString());


                        dialog.dismiss();
                        Snackbar snackbar = Snackbar
                                .make(constraintLayout, "บันทึกค่าพารามิเตอร์เรียบร้อย", Snackbar.LENGTH_SHORT);

                        snackbar.show();
                        //Toast.makeText(getApplicationContext(),"Dismissed..!!",Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show();
                return true;
            }
        });

        //Capture image button
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnCapture.setBackground(getResources().getDrawable(R.drawable.button_bg_round_clicked));
                captureLed.setVisibility(View.VISIBLE);
                MsgTextView.setText(""); //clear MsgTextView

                clearScore();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 100ms
                        btnCapture.setBackground(getResources().getDrawable(R.drawable.button_bg_round));
                        captureLed.setVisibility(View.GONE);
                    }
                }, 300);


                //if this is the last faceID image, that means the process is done.
                //So if capture image button is clicked again, it means user wants to do
                //faceID detection again
                if (processDone) {
                    realResult.setVisibility(View.GONE);
                    realResultTitle.setVisibility(View.GONE);
                    statusBar.setVisibility(View.VISIBLE);
                    countDownStartEnable = true;        //set countDown enable
                    processDone = false;
                }

                //if countDown enable, count down 4 second then take the first faceID image
                //otherwise, take photo as usual.
                if (countDownStartEnable) {
                    final Handler coundownHandler = new Handler();
                    coundownHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 100ms
                            countDownTextView.setVisibility(View.GONE);
                            referenceDotInfo.setVisibility(View.GONE);
                            takePicture();
                            startSequence = false;

                        }
                    }, 4000);
                } else {
                    takePicture();
                }

                referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame));
                if (countDownStartEnable && !startSequence) {
                    startSequence = true;
                }

                //hide snackbar when take photo
                if (snackbar != null) {
                    if (snackbar.isShown()) {
                        snackbar.dismiss();
                    }
                }

            }
        });

        //Submit all images to server
        asyncPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAnim();
                statusBar.setText("กำลังประมวลผล");
                new AsyncTaskForBase64JSONBuilder().execute();
            }
        });

    }

    private void setInitializeState() {
        btnCapture.setEnabled(false);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 10000ms
                InternetCheckingStatus checking = new InternetCheckingStatus(getApplicationContext());
                int internetStatus = checking.getInternetStatus();
                if (internetStatus != 0) {
                    statusBar.setText("ยินดีต้อนรับ");
                    internetStatusImage.setVisibility(View.VISIBLE);
                    btnCapture.setEnabled(true);
                    switch (internetStatus){
                        case 1:
                            internetStatusImage.setImageResource(R.drawable.wifi_icon18);
                        case 2:
                            internetStatusImage.setImageResource(R.drawable.cellular_icon18);
                    }
                } else {
                    internetStatusImage.setVisibility(View.INVISIBLE);
                }

                stopAnim();
            }
        }, 3000);

        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        initOkHttp3();
        setInitialVisibility();
        initialIntentFilter();
        initialClockTicking();

    }

    private void initialClockTicking() {
        Clock clockTicking = new Clock(getApplicationContext(), 0);
        clockTicking.addClockTickListener(new OnClockTickListener() {
            @Override
            public void onSecondTick(@NotNull GregorianCalendar currentTime) {

                //Prevent unintentional click from user that causes weird process
                if(duringFaceDetectionSequence){
                    spinner.setClickable(false);
                }else{
                    spinner.setClickable(true);
                }


                //check internet connection every 30s
                if (internetCheckTiming < 30) {
                    internetCheckTiming++;
                } else if (internetCheckTiming >= 30) {
                    InternetCheckingStatus checking = new InternetCheckingStatus(getApplicationContext());
                    int internetStatus = checking.getInternetStatus();
                    if (internetStatus != 0) {
                        internetStatusImage.setVisibility(View.VISIBLE);
                        switch (internetStatus){
                            case 1:
                                internetStatusImage.setImageResource(R.drawable.wifi_icon18);
                            case 2:
                                internetStatusImage.setImageResource(R.drawable.cellular_icon18);
                        }
                    } else {
                        internetStatusImage.setVisibility(View.INVISIBLE);
                    }
                    internetCheckTiming = 0;
                }


                //Count timing to 15 seconds
                //every 15 seconds, clear all snackbar
                if (timing < 15) {
                    timing++;
                } else {

                    timing = 0;
                    if (snackbar.isShown())
                        snackbar.dismiss();
                }

                //@ first 5 second, show snackbar for the first time no matter which mode is on
                if (timing == 5 && firstTimeToastSnackbar && !duringFaceDetectionSequence) {
                    //camera front
                    FaceIdSnackbar();
                    //first time toast is gone,
                    //no more first time condition
                    firstTimeToastSnackbar = false;

                }

                //Toast snackbar at 5 second of every loop
                if (timing == 5 && !firstTimeToastSnackbar && !duringFaceDetectionSequence) {
                    //camera front
                    if (snackbar != null) {
                        if (!snackbar.isShown()) {
                            FaceIdSnackbar();
                        }
                    }
                }

                // This part is for countDown faceID detection -> 4, 3, 2, 1, go
                countDownFaceID();

            }

            @Override
            public void onMinuteTick(@NotNull GregorianCalendar currentTime) {

            }
        });
    }

    private void initialIntentFilter() {
        filter = new IntentFilter();
        filter.addAction(SIGNAL_RECEIVER);
        receiver = new Base64SignalReceiver();

        photoFilter = new IntentFilter();
        photoFilter.addAction(PHOTO_TAKEN_RECEIVER);
        photoReceiver = new PhotoTakenSignalReceiver();


    }

    private void bindWidget() {
        textureView = findViewById(R.id.textureview);
        constraintLayout = findViewById(R.id.constraint_layout);
        internetStatusImage = findViewById(R.id.internet_status_img);
        avi = findViewById(R.id.avi);
        asyncPostBtn = findViewById(R.id.async_post);
        statusBar = findViewById(R.id.statusbar);
        referenceFrame = findViewById(R.id.reference_frame);
        textureView = findViewById(R.id.textureview);
        btnCapture = findViewById(R.id.btnCapture);
        captureLed = findViewById(R.id.capture_led);
        centerDot = findViewById(R.id.center_dot);
        topLeftDot = findViewById(R.id.topleft_dot);
        topRightDot = findViewById(R.id.topright_dot);
        bottomLeftDot = findViewById(R.id.bottomleft_dot);
        bottomRightDot = findViewById(R.id.bottomright_dot);
        middleLeftDot = findViewById(R.id.middleleft_dot);
        countDownTextView = findViewById(R.id.countdown_txtview);
        realResult = findViewById(R.id.realResult_txtview);
        realResultTitle = findViewById(R.id.realResult_title);
        referenceDotInfo = findViewById(R.id.reference_dot_info);
        errorCodeTextView = findViewById(R.id.errorcode_txtview);
        MsgTextView = findViewById(R.id.msg_txtview);
        ScoreTextView = findViewById(R.id.score_txtview);
        livenessTitle = findViewById(R.id.scorelive_title);
        chooseNumberTitle = findViewById(R.id.choosenumber_title);

        setSpinner();

    }

    private void setSpinner() {
        // Spinner element
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setGravity(Gravity.CENTER);
        // Spinner click listener
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0){
                    // 4 pics
                    pictureTakenMode = position;
                }else if(position == 1){
                    // 5 pics
                    pictureTakenMode = position;
                }else if(position == 2){
                    // 6 pics
                    pictureTakenMode = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Spinner Drop down elements
        List<String> categories = new ArrayList<String>();
        categories.add("4");
        categories.add("5");
        categories.add("6");

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
        spinner.setSelection(1);
    }

    private void setTypeFace(){
        Typeface regularFace = Typeface.createFromAsset(getAssets(),
                "PrintAble4U_Regular.ttf");
        Typeface boldFace = Typeface.createFromAsset(getAssets(),
                "PrintAble4U_Bold.ttf");
        Typeface droidSanBold = Typeface.createFromAsset(getAssets(),
                "DroidSans-Bold.ttf");

        asyncPostBtn.setTypeface(droidSanBold);
        statusBar.setTypeface(boldFace);
        statusBar.setTextSize(30);



        countDownTextView.setTypeface(droidSanBold);
        countDownTextView.setTextColor(getResources().getColor(R.color.metro_orange));
        countDownTextView.setBackground(getDrawable(R.drawable.countdown_background));
        countDownTextView.setTextSize(60);
        countDownTextView.setText("4");

        realResult.setTypeface(boldFace);
        realResult.setTextSize(30);

        realResultTitle.setTypeface(boldFace);
        realResultTitle.setTextSize(30);

        referenceDotInfo.setTextSize(30);
        referenceDotInfo.setTypeface(boldFace);
        referenceDotInfo.setTextColor(getResources().getColor(R.color.metro_yellow));
        referenceDotInfo.setText(getResources().getString(R.string.reference_dot_info_string));

        errorCodeTextView.setTypeface(droidSanBold);
        errorCodeTextView.setTextSize(10);

        MsgTextView.setTypeface(droidSanBold);
        MsgTextView.setTextColor(getResources().getColor(R.color.realRed));
        MsgTextView.setTextSize(15);
        MsgTextView.setText("");

        ScoreTextView.setTypeface(droidSanBold);
        ScoreTextView.setTextSize(15);
        ScoreTextView.setTextColor(getResources().getColor(R.color.whiteCreamCappucino));

        livenessTitle.setTextColor(getResources().getColor(R.color.whiteCreamCappucino));
        livenessTitle.setTypeface(droidSanBold);
        livenessTitle.setTextSize(18);

        chooseNumberTitle.setTypeface(droidSanBold);
        chooseNumberTitle.setTextColor(getResources().getColor(R.color.whiteCreamCappucino));
        chooseNumberTitle.setTextSize(15);
        chooseNumberTitle.setGravity(Gravity.CENTER);
        chooseNumberTitle.setText("เลือกจำนวน\nภาพถ่าย");

    }

    private void setInitialVisibility() {
        captureLed.setVisibility(View.GONE);
        realResult.setVisibility(View.INVISIBLE);
        realResultTitle.setVisibility(View.INVISIBLE);
        errorCodeTextView.setVisibility(View.INVISIBLE);
        countDownTextView.setVisibility(View.INVISIBLE);
        referenceDotInfo.setVisibility(View.INVISIBLE);

        ShowHideReferenceDot(false);
    }


    private void clearScore() {
        ScoreTextView.setText("0.0000");
    }

    void startAnim() {
        //avi.show();
        avi.setVisibility(View.VISIBLE);
        // or avi.smoothToShow();
    }

    void stopAnim() {
        //avi.hide();
        avi.setVisibility(View.INVISIBLE);
        // or avi.smoothToHide();
    }

    private void countDownFaceID() {
        //if countdown mode is on and sequence is started
        if (countDownStartEnable && startSequence) {
            //stop snackbar during countdown
            if (snackbar != null) {
                if (snackbar.isShown())
                    snackbar.dismiss();
            }

            duringFaceDetectionSequence = true;
            if (countDownPointer < 4) {
                countDownTextView.setText(String.valueOf(countDownSequence[countDownPointer]));
            }
            FadeOutSequence();
            if (countDownPointer < 4) {
                countDownPointer++;
            }
        }
    }

    //Fade out countdown textview
    private void FadeOutSequence() {
        countDownTextView.setVisibility(View.VISIBLE);
        referenceDotInfo.setVisibility(View.VISIBLE);
        Animation animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
        countDownTextView.startAnimation(animFadeOut);
        //show the first dot at the center
        ShowHideFaceIdReferenceDot(1);
    }

    //show and hide all reference dot at once.
    private void ShowHideReferenceDot(boolean visible) {
        if (visible) {
            centerDot.setVisibility(View.VISIBLE);
            topLeftDot.setVisibility(View.VISIBLE);
            topRightDot.setVisibility(View.VISIBLE);
            bottomLeftDot.setVisibility(View.VISIBLE);
            bottomRightDot.setVisibility(View.VISIBLE);
            middleLeftDot.setVisibility(View.VISIBLE);
        } else {
            centerDot.setVisibility(View.GONE);
            topLeftDot.setVisibility(View.GONE);
            topRightDot.setVisibility(View.GONE);
            bottomLeftDot.setVisibility(View.GONE);
            bottomRightDot.setVisibility(View.GONE);
            middleLeftDot.setVisibility(View.GONE);
        }
    }

    //show and hide each of reference dot in faceID capture process
    private void ShowHideFaceIdReferenceDot(int dotNumber) {
        if (dotNumber == 1) {
            centerDot.setVisibility(View.VISIBLE);
            topLeftDot.setVisibility(View.GONE);
            topRightDot.setVisibility(View.GONE);
            bottomLeftDot.setVisibility(View.GONE);
            bottomRightDot.setVisibility(View.GONE);
            middleLeftDot.setVisibility(View.GONE);
        } else if (dotNumber == 2) {
            centerDot.setVisibility(View.GONE);
            topLeftDot.setVisibility(View.VISIBLE);
            topRightDot.setVisibility(View.GONE);
            bottomLeftDot.setVisibility(View.GONE);
            bottomRightDot.setVisibility(View.GONE);
            middleLeftDot.setVisibility(View.GONE);
        } else if (dotNumber == 3) {
            centerDot.setVisibility(View.GONE);
            topLeftDot.setVisibility(View.GONE);
            topRightDot.setVisibility(View.VISIBLE);
            bottomLeftDot.setVisibility(View.GONE);
            bottomRightDot.setVisibility(View.GONE);
            middleLeftDot.setVisibility(View.GONE);
        } else if (dotNumber == 4) {
            centerDot.setVisibility(View.GONE);
            topLeftDot.setVisibility(View.GONE);
            topRightDot.setVisibility(View.GONE);
            bottomLeftDot.setVisibility(View.VISIBLE);
            bottomRightDot.setVisibility(View.GONE);
            middleLeftDot.setVisibility(View.GONE);
        } else if (dotNumber == 5) {
            centerDot.setVisibility(View.GONE);
            topLeftDot.setVisibility(View.GONE);
            topRightDot.setVisibility(View.GONE);
            bottomLeftDot.setVisibility(View.GONE);
            bottomRightDot.setVisibility(View.VISIBLE);
            middleLeftDot.setVisibility(View.GONE);
        } else if (dotNumber == 6){
            centerDot.setVisibility(View.GONE);
            topLeftDot.setVisibility(View.GONE);
            topRightDot.setVisibility(View.GONE);
            bottomLeftDot.setVisibility(View.GONE);
            bottomRightDot.setVisibility(View.GONE);
            middleLeftDot.setVisibility(View.VISIBLE);
        } else if (dotNumber == 7){
            centerDot.setVisibility(View.GONE);
            topLeftDot.setVisibility(View.GONE);
            topRightDot.setVisibility(View.VISIBLE);
            bottomLeftDot.setVisibility(View.GONE);
            bottomRightDot.setVisibility(View.GONE);
            middleLeftDot.setVisibility(View.GONE);
        }
    }

    //Toast snackbar when faceID mode is on
    private void FaceIdSnackbar() {
        snackbar = TSnackbar.make(constraintLayout,
                "ถ่ายภาพผู้ใช้งานหน้าตรงภายในกรอบที่กำหนด",
                TSnackbar.LENGTH_INDEFINITE);
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.setIconLeft(R.drawable.user_40, 24); //Size in dp - 24 is great!
        snackbar.setIconPadding(8);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(R.color.firstShadeOfDarkGreenGoogle));
        Typeface regularFace = Typeface.createFromAsset(getAssets(),
                "PrintAble4U_Regular.ttf");

        TextView textView = (TextView) snackbarView.findViewById(com.androidadvance.topsnackbar.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(regularFace);
        textView.setTextSize(20);
        snackbar.show();
    }

    private void takePicture() {
        if (cameraDevice == null) {
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());

            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

                //Capture image with custom size
                int width = 640;
                int height = 480;


                if (jpegSizes != null && jpegSizes.length > 0) {
                    width = jpegSizes[0].getWidth();
                    height = jpegSizes[0].getHeight();

                }
                ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
                List<Surface> outputSurface = new ArrayList<>(2);

                outputSurface.add(reader.getSurface());
                outputSurface.add(new Surface(textureView.getSurfaceTexture()));

                final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

                captureBuilder.addTarget(reader.getSurface());


                //Take photo with auto focus
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                // Use the same AE and AF modes as the preview.
                //captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                //        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);



                //Chack orientation base on device
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

                //save all of image file to 'liveness' folder
                //File sd = new File(Environment.getExternalStorageDirectory() + "/liveness/");
                File sd = new File(Environment.getExternalStorageDirectory() + "/liveness_test/");
                //Log.d(TAG, "path exits-------------------------" + String.valueOf(sd.exists()));
                if (!sd.exists()) {

                    sd.mkdir();
                }
                SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
                String format = s.format(new Date());

                //file = new File(sd, UUID.randomUUID().toString() + ".jpg");
                file = new File(sd, format + ".jpg");
                ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {

                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = null;
                        try {
                            image = reader.acquireLatestImage();
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            save(bytes);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (image != null) {
                                image.close();
                            }
                        }
                    }

                    private void save(byte[] bytes) throws IOException {
                        OutputStream outputStream = null;
                        try {
                            outputStream = new FileOutputStream(file);
                            outputStream.write(bytes);
                        } finally {
                            if (outputStream != null) {
                                outputStream.close();


                                //Async task for image to base64 converter
                                //new AsyncBase64WorkerTask(file).execute();

                                new AsyncTaskForImageFilePath(file).execute();
                            }
                        }
                    }
                };

                reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
                final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        //Toast.makeText(getApplicationContext(), "Saved " + file , Toast.LENGTH_SHORT).show();
                        createCameraPreview();


                    }
                };

                cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);

                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                    }
                }, mBackgroundHandler);

            }


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    class AsyncTaskForImageFilePath extends AsyncTask<Integer, Void, String> {

        private final File file;

        // Constructor
        public AsyncTaskForImageFilePath(File file) {
            this.file = file;
        }

        // Compress and Decode image in background.
        @Override
        protected String doInBackground(Integer... params) {

                if (count == 0) {
                    fivePicture_filePath[0] = file.getPath();
                }else if(count == 1){
                    fivePicture_filePath[1] = file.getPath();
                }else if(count == 2){
                    fivePicture_filePath[2] = file.getPath();
                }else if(count == 3){
                    fivePicture_filePath[3] = file.getPath();
                }else if(count == 4){
                    fivePicture_filePath[4] = file.getPath();
                }else if(count == 5){
                    fivePicture_filePath[5] = file.getPath();
                }else if(count == 6){
                    fivePicture_filePath[6] = file.getPath();
                }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            //After image is converted to base64
            //Log.d(TAG, "onPost execute: " + string);
            //if smart card capture mode is on, save base64 string to 'cardPicture_base64' variable

            if(pictureTakenMode == 0){
                // 4 pics (+1 straight)
                if (count == 0) {
                    //fivePicture_base64[0] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 1: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(2);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 1) {
                    //fivePicture_base64[1] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 2: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(3);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 2) {
                    //fivePicture_base64[2] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 3: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(4);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 3) {
                    //fivePicture_base64[3] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 4: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(5);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 4) {
                    //fivePicture_base64[3] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 5: บันทึก");
                    setFlagForLastFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideReferenceDot(false);



                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 2000ms
                            statusBar.setText("ตรวจสอบภาพถ่าย");
                            //set face id reference frame to normal
                            referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame));
                            //submit all 5 face images + 1 smartcard image to server


                            new AsyncBase64WorkerTask(fivePicture_filePath[0], "face0").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[1], "face1").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[2], "face2").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[3], "face3").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[4], "face4").execute();
                            //asyncPostBtn.performClick();

                        }
                    }, 1000);

                }
            }else if(pictureTakenMode == 1){
                // 5 pics (+1 straight)
                if (count == 0) {
                    //fivePicture_base64[0] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 1: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(2);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 1) {
                    //fivePicture_base64[1] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 2: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(3);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 2) {
                    //fivePicture_base64[2] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 3: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(4);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 3) {
                    //fivePicture_base64[3] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 4: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(5);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 4) {
                    //fivePicture_base64[3] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 5: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(6);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                }else if (count == 5) {
                    //fivePicture_base64[4] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 6: บันทึก");
                    setFlagForLastFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideReferenceDot(false);



                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 2000ms
                            statusBar.setText("ตรวจสอบภาพถ่าย");
                            //set face id reference frame to normal
                            referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame));
                            //submit all 5 face images + 1 smartcard image to server


                            new AsyncBase64WorkerTask(fivePicture_filePath[0], "face0").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[1], "face1").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[2], "face2").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[3], "face3").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[4], "face4").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[5], "face5").execute();
                            //asyncPostBtn.performClick();

                        }
                    }, 1000);

                }
            }else if(pictureTakenMode == 2){
                // 6 pics (+1 straight)
                if (count == 0) {
                    //fivePicture_base64[0] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 1: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(2);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 1) {
                    //fivePicture_base64[1] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 2: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(3);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 2) {
                    //fivePicture_base64[2] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 3: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(4);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 3) {
                    //fivePicture_base64[3] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 4: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(5);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 4) {
                    //fivePicture_base64[3] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 5: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(6);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 5) {
                    //fivePicture_base64[3] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 6: บันทึก");
                    setFlagDuringFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideFaceIdReferenceDot(7);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 500ms
                            //btnCapture.performClick();
                            Intent intent = new Intent();
                            intent.setAction(PHOTO_TAKEN_RECEIVER);
                            sendBroadcast(intent);

                        }
                    }, 1000);

                } else if (count == 6) {
                    //fivePicture_base64[4] = string;
                    statusBar.setVisibility(View.VISIBLE);
                    statusBar.setText("ตรวจจับใบหน้าจุดที่ 7: บันทึก");
                    setFlagForLastFaceID();


                    //check if camera still front and capture face mode is already on
                    referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame_passed));
                    ShowHideReferenceDot(false);



                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 2000ms
                            statusBar.setText("ตรวจสอบภาพถ่าย");
                            //set face id reference frame to normal
                            referenceFrame.setImageDrawable(getResources().getDrawable(R.drawable.faceid_frame));
                            //submit all 5 face images + 1 smartcard image to server


                            new AsyncBase64WorkerTask(fivePicture_filePath[0], "face0").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[1], "face1").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[2], "face2").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[3], "face3").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[4], "face4").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[5], "face5").execute();
                            new AsyncBase64WorkerTask(fivePicture_filePath[6], "face6").execute();
                            //asyncPostBtn.performClick();

                        }
                    }, 1000);

                }
            }



        }

        private void setFlagDuringFaceID() {
            //count++;        // count 1 for the next faceID image
            countDownStartEnable = false;   //Turn off countdown
            countDownPointer = 0;           //reset countdown pointer
            duringFaceDetectionSequence = true; //set this flag to prevent toast snackbar during process

        }

        private void setFlagForLastFaceID() {
            count = 0;
            countDownStartEnable = false;
            countDownPointer = 0;
            duringFaceDetectionSequence = true;
            processDone = true;
        }
    }

    //Asynctask for base64 converter
    class AsyncBase64WorkerTask extends AsyncTask<Integer, Void, String> {
        private final String filePath;
        private String imageTag;

        // Constructor
        public AsyncBase64WorkerTask(String filePath, String imageTag) {
            this.filePath = filePath;
            this.imageTag = imageTag;
        }

        // Compress and Decode image in background.
        @Override
        protected String doInBackground(Integer... params) {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            //bitmap = toGrayscale(bitmap);
            //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            bitmap = toGrayscale(bitmap);
            //saveImageJpeg(bitmap);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            byte[] imageBytes = stream.toByteArray();
            String image_str = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            //.String image_str = "";
            return image_str;
        }

        // This method is run on the UI thread
        @Override
        protected void onPostExecute(String string) {
            //After image is converted to base64
            //Log.d(TAG, "onPost execute: " + string);
            if(pictureTakenMode == 0){
                // 4 pics + 1 straight
                if(imageTag.equals("face0")){
                    fivePicture_base64[0] = string;Log.i(TAG, fivePicture_base64[0]);
                }else if(imageTag.equals("face1")){
                    fivePicture_base64[1] = string;Log.i(TAG, fivePicture_base64[1]);
                }else if(imageTag.equals("face2")){
                    fivePicture_base64[2] = string;Log.i(TAG, fivePicture_base64[2]);
                }else if(imageTag.equals("face3")) {
                    fivePicture_base64[3] = string;Log.i(TAG, fivePicture_base64[3]);
                }else if(imageTag.equals("face4")){
                    fivePicture_base64[4] = string;Log.i(TAG, fivePicture_base64[4]);
                    Intent intent = new Intent();
                    intent.setAction(SIGNAL_RECEIVER);
                    sendBroadcast(intent);
                }
            }else if(pictureTakenMode == 1){
                // 5 pics + 1 straight
                if(imageTag.equals("face0")){
                    fivePicture_base64[0] = string;Log.i(TAG, fivePicture_base64[0]);
                }else if(imageTag.equals("face1")){
                    fivePicture_base64[1] = string;Log.i(TAG, fivePicture_base64[1]);
                }else if(imageTag.equals("face2")){
                    fivePicture_base64[2] = string;Log.i(TAG, fivePicture_base64[2]);
                }else if(imageTag.equals("face3")) {
                    fivePicture_base64[3] = string;Log.i(TAG, fivePicture_base64[3]);
                }else if(imageTag.equals("face4")){
                    fivePicture_base64[4] = string;Log.i(TAG, fivePicture_base64[4]);

                }else if(imageTag.equals("face5")){
                    fivePicture_base64[5] = string;Log.i(TAG, fivePicture_base64[5]);

                    Intent intent = new Intent();
                    intent.setAction(SIGNAL_RECEIVER);
                    sendBroadcast(intent);
                }
            }else if(pictureTakenMode == 2){
                // 6 pics  + 1 straight
                if(imageTag.equals("face0")){
                    fivePicture_base64[0] = string;Log.i(TAG, fivePicture_base64[0]);
                }else if(imageTag.equals("face1")){
                    fivePicture_base64[1] = string;Log.i(TAG, fivePicture_base64[1]);
                }else if(imageTag.equals("face2")){
                    fivePicture_base64[2] = string;Log.i(TAG, fivePicture_base64[2]);
                }else if(imageTag.equals("face3")) {
                    fivePicture_base64[3] = string;Log.i(TAG, fivePicture_base64[3]);
                }else if(imageTag.equals("face4")){
                    fivePicture_base64[4] = string;Log.i(TAG, fivePicture_base64[4]);
                }else if(imageTag.equals("face5")){
                    fivePicture_base64[5] = string;Log.i(TAG, fivePicture_base64[5]);
                }else if(imageTag.equals("face6")){
                    fivePicture_base64[6] = string;Log.i(TAG, fivePicture_base64[6]);

                    Intent intent = new Intent();
                    intent.setAction(SIGNAL_RECEIVER);
                    sendBroadcast(intent);
                }
            }

        }
    }















    class AsyncTaskForBase64JSONBuilder extends AsyncTask<Integer, Void, String> {

        private final String url = "http://171.100.69.126:8800/livenessonly"; //Liveness API path
        private String responseMsg = null;
        private Call call;

        // Constructor
        public AsyncTaskForBase64JSONBuilder() {

        }

        @Override
        protected String doInBackground(Integer... integers) {

            try{
                // Create okhttp3.Call object with post http request method.
                call = createHttpPostMethodCall(url);
                // Execute the request and get the response asynchronously.
                //call.execute();
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        sendChildThreadMessageToMainThread("Asynchronous http post request failed.");
                        Log.d(TAG, "Response error: " + e);
                        if(e.toString().contains("java.net.SocketTimeoutException")){
                            //Toast.makeText(getApplicationContext(), "ส่งคำร้องขอไม่สำเร็จ", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        if (response.isSuccessful()) {
                            Log.d(TAG, "Response success: " + String.valueOf(response));
                            responseMsg = String.valueOf(response);
                            // Parse and get server response text data.
                            String respData = parseResponseText(response);

                            // Notify activity main thread to update UI display text with Handler.
                            sendChildThreadMessageToMainThread(respData);
                        } else {
                            Log.d(TAG, "Response not success: " + String.valueOf(response));
                        }
                    }
                });
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                sendChildThreadMessageToMainThread(ex.getMessage());
                stopAnim();
            }
            return responseMsg;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s != null){
                Log.d(TAG, s);
            }

        }
    }
    private void initOkHttp3() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient();

            //set timeout for 30 second for server response
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(60, TimeUnit.SECONDS);
            builder.readTimeout(60, TimeUnit.SECONDS);
            builder.writeTimeout(60, TimeUnit.SECONDS);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

            okHttpClient = builder.addNetworkInterceptor(logging).build();


        }
        if (displayRespTextHandler == null) {
            displayRespTextHandler = new Handler() {
                // When this handler receive message from child thread.
                @Override
                public void handleMessage(Message msg) {

                    // Check what this message want to do.
                    if (msg.what == COMMAND_DISPLAY_SERVER_RESPONSE) {
                        // Get server response text.
                        Bundle bundle = msg.getData();
                        String respText = bundle.getString(KEY_SERVER_RESPONSE_OBJECT);

                        Log.d(TAG, "Response handleMessage: " + respText);
                        statusBar.setText("พร้อมใช้งาน");
                        stopAnim();
                        try {
                            JSONObject json = new JSONObject(respText);
                            int errorcode = json.getInt("ErrorCode");
                            errorCodeTextView.setText("ErrorCode: " + String.valueOf(errorcode));
                            if (errorcode == 0) {
                                realResult.setVisibility(View.VISIBLE);
                                realResultTitle.setVisibility(View.VISIBLE);
                                statusBar.setVisibility(View.INVISIBLE);
                                //statusBar.setText("ผลการตรวจสอบ:");
                                realResult.setText("ผ่าน");
                                realResult.setTextColor(getResources().getColor(R.color.greenGoogle));
                            } else if (errorcode == 1) {
                                realResult.setVisibility(View.VISIBLE);
                                realResultTitle.setVisibility(View.VISIBLE);
                                statusBar.setVisibility(View.INVISIBLE);
                                //statusBar.setText("ผลการตรวจสอบ:");
                                realResult.setText("ไม่ผ่าน");
                                realResult.setTextColor(getResources().getColor(R.color.redGoogle));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                        try {
                            JSONObject json = new JSONObject(respText);
                            //Passed
                            String message = json.getString("Msg");
                            if (message.equals("liveness")) {
                                MsgTextView.setText("");
                            } else {
                                MsgTextView.setText(message);
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            JSONObject json = new JSONObject(respText);
                            String liveness = json.getString("LivenessScore");
                            ScoreTextView.setText(
                                    String.format("%.4f", Double.parseDouble(liveness)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            JSONObject json = new JSONObject(respText);
                            double liveness = json.getDouble("Score");
                            NumberFormat formatter = new DecimalFormat("#.####");
                            formatter.setRoundingMode(RoundingMode.FLOOR);
                            String resultNumber = formatter.format(liveness);
                            ScoreTextView.setText(resultNumber);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    //set this flag to enable snackbar again
                    duringFaceDetectionSequence = false;
                }
            };
        }
    }
    /* Create OkHttp3 Call object use post method with url. */
    private Call createHttpPostMethodCall(String url) {
        // Create okhttp3 form body builder.

        JSONObject jsonObject = new JSONObject();
        JSONObject jsonEyeDistance = new JSONObject();
        //JSONObject jsonCardEyeDistance = new JSONObject();

        // create POST request in JSON format
        //1. Eyedistance 'max','min'
        try {
            jsonEyeDistance.put("max", EyeDistanceMax);
            jsonEyeDistance.put("min", EyeDistanceMin);
            jsonObject.put("eyeDistance", jsonEyeDistance);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //2. CardEyeDistance 'max', 'min'
        /*try {
            jsonCardEyeDistance.put("max", CardEyeDistanceMax);
            jsonCardEyeDistance.put("min", CardEyeDistanceMin);
            jsonObject.put("CardeyeDistance", jsonCardEyeDistance);
        }catch (JSONException e) {
            e.printStackTrace();
        }*/


        //4. 5 faceID images in base64 string
        JSONArray jsonArr = new JSONArray();

        if(pictureTakenMode == 0){
            // 4 pics
            for (int i = 1; i < 5; i++) {
                JSONObject sgObj = new JSONObject();
                try {
                    sgObj.put("image", fivePicture_base64[i]);
                    sgObj.put("dotPosition", dotPosition4[i]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                jsonArr.put(sgObj);
            }
        }else if(pictureTakenMode == 1){
            // 5 pics
            for (int i = 1; i < 6; i++) {
                JSONObject sgObj = new JSONObject();
                try {
                    sgObj.put("image", fivePicture_base64[i]);
                    sgObj.put("dotPosition", dotPosition5[i]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                jsonArr.put(sgObj);
            }
        }else if(pictureTakenMode == 2){
            // 6 pics
            for (int i = 1; i < 7; i++) {
                JSONObject sgObj = new JSONObject();
                try {
                    sgObj.put("image", fivePicture_base64[i]);
                    sgObj.put("dotPosition", dotPosition6[i]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                jsonArr.put(sgObj);
            }
        }


        try {
            jsonObject.put("segments", jsonArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, jsonObject.toString());
        Log.d(TAG, "JSON body: " + jsonObject.toString());
        // Create a http request object.

        Request.Builder builder = new Request.Builder();
        builder = builder.url(url);
        builder = builder.post(body);
        Request request = builder.build();

        Log.d(TAG, "Post request: " + request.toString());

        // Create a new Call object with post method.
        Call call = okHttpClient.newCall(request);

        return call;
    }

    /* Parse response code, message, headers and body string from server response object. */
    private String parseResponseText(Response response) {
        // Get response code.
        int respCode = response.code();

        // Get message
        String respMsg = response.message();

        // Get headers.
        List<String> headerStringList = new ArrayList<String>();

        Headers headers = response.headers();
        Map<String, List<String>> headerMap = headers.toMultimap();
        Set<String> keySet = headerMap.keySet();
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            String headerKey = it.next();
            List<String> headerValueList = headerMap.get(headerKey);

            StringBuffer headerBuf = new StringBuffer();
            headerBuf.append(headerKey);
            headerBuf.append(" = ");

            for (String headerValue : headerValueList) {
                headerBuf.append(headerValue);
                headerBuf.append(" , ");
            }

            headerStringList.add(headerBuf.toString());
        }

        // Get body text.
        String respBody = "";
        try {
            respBody = response.body().string();
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        String returner = respBody;
        return returner;
    }

    // Send message from child thread to activity main thread.
    // Because can not modify UI controls in child thread directly.
    private void sendChildThreadMessageToMainThread(String respData) {
        // Create a Message object.
        Message message = new Message();

        // Set message type.
        message.what = COMMAND_DISPLAY_SERVER_RESPONSE;

        // Set server response text data.
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SERVER_RESPONSE_OBJECT, respData);
        message.setData(bundle);

        // Send message to activity Handler.
        displayRespTextHandler.sendMessage(message);
    }














    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuidler = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuidler.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null)
                        return;
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
        captureRequestBuidler.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);


        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuidler.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void OpenCamara(int cameraSelected) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[cameraSelected];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(String.valueOf(CAMERA_FRONT));
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //check realtime permission if run higher API 23
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallBack, null);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            OpenCamara(cameraSelected);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "You can't use camera without permission",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            OpenCamara(cameraSelected);
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }

        registerReceiver(receiver, filter);
        registerReceiver(photoReceiver, photoFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(photoReceiver);
    }

    @Override
    protected void onPause() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

    }































    public class Base64SignalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // assumes WordService is a registered service
            //Toast.makeText(context, "BroadcastReceiver activated", Toast.LENGTH_SHORT).show();
            asyncPostBtn.performClick();

        }
    }

    public class PhotoTakenSignalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // assumes WordService is a registered service
            //Toast.makeText(context, "BroadcastReceiver activated", Toast.LENGTH_SHORT).show();
            if(pictureTakenMode == 0){
                if(count < 5){
                    btnCapture.performClick();
                    count++;
                }
            }else if(pictureTakenMode == 1){
                if(count < 6){
                    btnCapture.performClick();
                    count++;
                }
            }else if(pictureTakenMode == 2){
                if(count < 7){
                    btnCapture.performClick();
                    count++;
                }
            }




        }
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}
