package com.example.vision;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static org.opencv.core.CvType.CV_8UC3;


public class DetectionActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "opencv";

    private Net net;
    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};


    private CameraBridgeViewBase mOpenCvCameraView;

    // assets directory의 mobileNet_SSD의 file 정보를 받아오기위한 함수
    // OutputStream을 통해 storage/emulated에 저장해 놓는듯, 마치 임시 file같은 느낌인가?
    // file을 copy해 앱이 돌아가는 동안 가상의 file에 복사해 놓는듯.. return값은 가상의 file의 경로.
    private static String copyFile(String filename, Context context) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = context.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }

        return pathDir;
    }

    // 아마도 loader Callback
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    //-------------------------------- life Cycle --------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_detection);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    // ---------------------------- opencv의 life cycle시에 자동으로 호출되는 함수 implements class Override 함수 ----------------------------
    // camera view가 시작할때 딥뉴럴 네트워크 설정!!
    @Override
    public void onCameraViewStarted(int width, int height) {
        String pro = copyFile("MobileNetSSD_deploy.prototxt.txt", this);
        String caff = copyFile("MobileNetSSD_deploy.caffemodel", this);
        net = Dnn.readNetFromCaffe(pro, caff);
    }

    @Override
    public void onCameraViewStopped() {

    }

    // 이미지 사이즈 변경
    public float resize(Mat img_src, Mat img_resize, int resize_width){

        float scale = resize_width / (float)img_src.cols() ;
        if (img_src.cols() > resize_width) {
            int new_height = Math.round(img_src.rows() * scale);
            Imgproc.resize(img_src, img_resize, new Size(resize_width, new_height));
        }
        else {
            img_resize = img_src;
        }
        return scale;
    }

    /*
    opencv 화면 조작
    DNN을 실행시키고 결과에 따라 화면을 그린다.
    이 함수가 view를 셋팅하지 않고도 callback으로 잘 실행되는지 실험해봐야한다.
    그러면 inputFrame을 조작해서 service에 이용할수 있다.
    */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 0.007843;
        final double MEAN_VAL = 127.5;
        final double THRESHOLD = 0.2;

        // Get a new frame _ Mat이 rgb값의 opencv 행렬값을 가진 자료형인듯..
        Mat frame = inputFrame.rgba();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

        Mat frame_resized = new Mat(frame.rows(), frame.cols(), CV_8UC3, new Scalar(255, 255, 255));
        float resizeRatio = resize(frame, frame_resized, 160);


        // Forward image through network.
        // blob은 평균, 분산등으로 전처리 하는 함수 인듯..
        Mat blob = Dnn.blobFromImage(frame_resized, IN_SCALE_FACTOR,
                new Size(IN_WIDTH, IN_HEIGHT),
                new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), /*swapRB*/false, /*crop*/false);

        // deep neural network에 forward
        net.setInput(blob);
        Mat detections = net.forward();
        int cols = frame_resized.cols();
        int rows = frame_resized.rows();
        detections = detections.reshape(1, (int)detections.total() / 7);


        for (int i = 0; i < detections.rows(); ++i) {
            double confidence = detections.get(i, 2)[0];
            if (confidence > THRESHOLD) {
                int classId = (int)detections.get(i, 1)[0];
                int left   = (int)((detections.get(i, 3)[0]* cols)/resizeRatio );
                int top    = (int)((detections.get(i, 4)[0]* rows)/resizeRatio );
                int right  = (int)((detections.get(i, 5)[0]* cols)/resizeRatio );
                int bottom = (int)((detections.get(i, 6)[0]* rows)/resizeRatio );


                // Draw rectangle around detected object.
                Imgproc.rectangle(frame, new Point(left, top), new Point(right, bottom),
                        new Scalar(0, 255, 0));
                String label = classNames[classId] + ": " + String.format("%,4.2f", confidence);;
                int[] baseLine = new int[1];
                Size labelSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_SIMPLEX, 2.5, 5, baseLine);
                // Draw background for label.
                Imgproc.rectangle(frame, new Point(left, top - labelSize.height),
                        new Point(left + labelSize.width, top + baseLine[0]),
                        new Scalar(255, 255, 255), Imgproc.FILLED);
                // Write class name and confidence.
                Imgproc.putText(frame, label, new Point(left, top),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 2.5, new Scalar(0, 0, 0));
            }
        }
        return frame;
    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    //----------------------------- Permission Method -----------------------------
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;

        // Camera 권한 체크!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( DetectionActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }


}
