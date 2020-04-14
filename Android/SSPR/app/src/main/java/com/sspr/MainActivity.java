package com.sspr;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.sspr.Camera.Camera2Fragment;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {


    private static final String DETECTION_MODEL_FILE = "detect.tflite";
    private static final String DETECTION_LABEL_FILE = "file:///android_asset/labels.txt";
    private static int DETECTION_IMAGE_SIZE = 320;
    private static Boolean IS_DETECTION_MODEL_QUANTISED = false;

    private static final String CHARACTER_MODEL_FILE = "fine_character_model.tflite";
    private static final String CHARACTER_LABEL_FILE = "file:///android_asset/character_labels.txt";
    public static int CHARACTER_IMAGE_SIZE = 20;
    public static int CHARACTER_IMAGE_PADDING = 4;
    private static Boolean IS_CHARACTER_MODEL_QUANTISED = false;

    static {
        if (OpenCVLoader.initDebug()){
            Log.d("MainActivity", "OpenCV loaded successfully");
        }
        else {
            Log.d("MainActivity", "OpenCV not loaded");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (null == savedInstanceState || true) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container,
                            Camera2Fragment.newInstance(getAssets(), DETECTION_MODEL_FILE,
                                    DETECTION_LABEL_FILE, DETECTION_IMAGE_SIZE,
                                    IS_DETECTION_MODEL_QUANTISED,
                                    CHARACTER_MODEL_FILE, CHARACTER_LABEL_FILE,
                                    CHARACTER_IMAGE_SIZE, CHARACTER_IMAGE_PADDING,
                                    IS_CHARACTER_MODEL_QUANTISED))
                    .commit();
        }
    }
}