package com.cheehong.changedetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.WindowManager;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG              = "MainActivity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat                  mIntermediateMat;

    private Mat current;
    private Mat previous;
    private Mat difference;//difference between previous and current

    private boolean first = true;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private boolean isPermissionChecked = false;
    private boolean isPermissionGranted = false;


    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = findViewById(R.id.change_detector_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);


    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(!isPermissionChecked) {
            checkPermission(Manifest.permission.CAMERA);
            isPermissionChecked = true;
        }
        if(isPermissionGranted)
        {
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }


    public void onCameraViewStarted(int width, int height) {
        previous = new Mat(width, height, CvType.CV_64FC4);
        current = new Mat(width, height, CvType.CV_64FC4);
        difference = new Mat(width, height, CvType.CV_64FC4);
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null) {
            mIntermediateMat.release();
        }

        mIntermediateMat = null;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        current = inputFrame.rgba();
        if(first){//first is true at the first frame
            previous = inputFrame.rgba();
            first = false;
        }

        Core.absdiff(current, previous, difference);
        previous = inputFrame.rgba();

        return difference;
    }


    //  check and request permission for camera
    public void checkPermission(String permission)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, CAMERA_PERMISSION_CODE);
        }
        else {
            Log.i(TAG, "Permission already granted");
            isPermissionGranted = true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Camera Permission Granted");

        }
        else {
            Log.i(TAG, "Camera Permission Denied");
        }
        isPermissionGranted = true;
    }

}
