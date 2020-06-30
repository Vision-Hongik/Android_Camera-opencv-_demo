package com.example.vision;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private Button gpsButton;
    private Button cameraButton;
    private Button detectButton;
    private Button submitButton;
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gpsButton = (Button) findViewById(R.id.gpsAct);
        cameraButton = (Button) findViewById(R.id.cameraAct);
        detectButton = (Button) findViewById(R.id.detectAct);
        submitButton = (Button) findViewById(R.id.submitAct);
        // GPS page Intent
        gpsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),GpsActivity.class);
                startActivity(intent);//액티비티 띄우기
            }
        });

        // CAMERA page Intent
        cameraButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),CameraActivity.class);
                startActivity(intent);//액티비티 띄우기
            }
        });

        // DETECTION page Intent
        detectButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),DetectionActivity.class);
                startActivity(intent);//액티비티 띄우기
            }
        });

        // SUBMIT TO SERVER page Intent
        submitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),SubmitActivity.class);
                startActivity(intent);//액티비티 띄우기
            }
        });
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI();
}
