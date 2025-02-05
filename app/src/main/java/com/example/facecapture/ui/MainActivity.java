package com.example.facecapture.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.widget.Toast;

import com.cubox.face_camera.camera.FaceCamera;
import com.cubox.face_camera.camera.FaceCameraFactory;
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
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initializeCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initializeCamera() {
        camera = FaceCameraFactory.create(this);
        camera.initialize();

        camera.startCamera(binding.viewPreview, this);
        camera.takePictureWithDelay(this, 3000);
    }


    @Override
    public void onImageSaved(File file) {
        Toast.makeText(this, "Image saved: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//        camera.saveFileToGallery(file, this);
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, "Image saved error: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onImageSavedToGallery(String message) {
        Toast.makeText(this, "Image saved to gallery: " + message, Toast.LENGTH_SHORT).show();
    }
}
