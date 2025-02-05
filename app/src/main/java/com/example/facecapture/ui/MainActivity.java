package com.example.facecapture.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.widget.Toast;

import com.example.facecapture.camera.FaceCamera;
import com.example.facecapture.camera.FaceCameraFactory;
import com.example.facecapture.databinding.ActivityMainBinding;

import java.io.File;

public class MainActivity extends AppCompatActivity implements FaceCamera.onImageSavedListener {
    private ActivityMainBinding binding;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FaceCamera camera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
        checkCameraPermission();
    }

    private void initView() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                initializeCamera();
            } else {
                finish();
            }
        });

        // CircleOverlayView를 상단에 그려줌
        FrameLayout rootLayout = findViewById(android.R.id.content);
        CircleOverlayView circleOverlayView = new CircleOverlayView(this);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        rootLayout.addView(circleOverlayView, params);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initializeCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initializeCamera() {
        camera = FaceCameraFactory.create();
        camera.initialize(this);

        camera.startCamera(binding.viewPreview, this);

        binding.viewPreview.setOnClickListener(v -> {
            camera.takePicture(this);
        });
    }


    @Override
    public void onImageSaved(File file) {
        Toast.makeText(this, "Image saved: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, "Image saved error: " + message, Toast.LENGTH_SHORT).show();
    }
}
