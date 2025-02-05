package com.example.facecapture.camera;

import android.content.Context;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

public interface FaceCamera {
    void initialize(Context context);
    void startCamera(PreviewView viewPreview, LifecycleOwner lifecycleOwner);
    void takePicture(ShowMessageCallback showMessage);
    PreviewView getPreviewView();

    @FunctionalInterface
    interface ShowMessageCallback {
        void showMessage(String message);
    }
}
