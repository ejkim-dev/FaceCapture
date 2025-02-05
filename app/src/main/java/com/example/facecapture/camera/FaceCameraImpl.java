package com.example.facecapture.camera;

import android.content.Context;
import android.os.Environment;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;


class FaceCameraImpl implements FaceCamera {
    private Context context;
    private Camera camera;
    private PreviewView previewView;
    private Preview preview;
    private ProcessCameraProvider provider;
    private ExecutorService executor;
    private MediaStoreOutputOptions mediaStoreOutput;

    private ImageCapture imageCapture;
    @Override
    public void initialize(Context context) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        previewView = new PreviewView(context);
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        try {
            provider = cameraProviderFuture.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        imageCapture = new ImageCapture.Builder().build();
        this.context = context;
    }

    @Override
    public void startCamera(PreviewView viewPreview, LifecycleOwner lifecycleOwner) {
        provider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(viewPreview.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);
    }

    @Override
    public void takePicture(ShowMessageCallback showMessage) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/FaceCapture");
        if (!path.exists()) path.mkdirs();
        File photoFile = new File(
                path, new SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.KOREA
        ).format(new Date()) + ".jpg"
        );
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                showMessage.showMessage("사진이 저장되었습니다.");
            }

            @Override
            public void onError(ImageCaptureException exception) {
                showMessage.showMessage("사진 저장에 실패했습니다.");
            }
        });
    }

    @Override
    public PreviewView getPreviewView() {
        return previewView;
    }
}
