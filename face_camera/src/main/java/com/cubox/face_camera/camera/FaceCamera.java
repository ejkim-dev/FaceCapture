package com.cubox.face_camera.camera;

import android.content.Context;

import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;

public interface FaceCamera {
    void initialize();
    void startCamera(PreviewView viewPreview, LifecycleOwner lifecycleOwner);
    void takePicture(onImageSavedListener listener);
    void takePictureWithDelay(onImageSavedListener listener, int delayMillis);
    void saveFileToGallery(File cacheFile, onImageSavedListener listener);
    PreviewView getPreviewView();

    interface onImageSavedListener {
        void onImageSaved(File file);
        void onImageSavedToGallery(String message);
        void onError(String message);
    }
}
