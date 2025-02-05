package com.example.facecapture.camera;

import android.content.Context;

import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;

public interface FaceCamera {
    void initialize(Context context);
    void startCamera(PreviewView viewPreview, LifecycleOwner lifecycleOwner);
    void takePicture(onImageSavedListener listener);
    PreviewView getPreviewView();

    interface onImageSavedListener {
        void onImageSaved(File file);
        void onError(String message);
    }
}
