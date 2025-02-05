package com.example.facecapture.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;


class FaceCameraImpl implements FaceCamera {
    private Context context;
    private PreviewView previewView;
    private Preview preview;
    private ProcessCameraProvider provider;
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
    public void takePicture(onImageSavedListener listener) {
        String fileName = "_faceCamera_" + System.currentTimeMillis() + ".jpg";

        // 이미지를 캐시에 저장
        File cacheDir = new File(context.getCacheDir(), fileName);
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(cacheDir).build();

        imageCapture.takePicture(
                outputFileOptions,
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap bitmap = rotateBitmap(outputFileResults);
                        saveRotationImage(bitmap, new OnFileSavedListener() {
                            @Override
                            public void onFileSaved(File file) {
                                listener.onImageSaved(file);
                            }

                            @Override
                            public void onError(Exception e) {
                                listener.onError("사진 저장에 실패했습니다.");
                            }
                        });

                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        listener.onError("사진 저장에 실패했습니다.");
                    }
                });
    }

    @Override
    public PreviewView getPreviewView() {
        return previewView;
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(context);
    }

    private void saveRotationImage(Bitmap bitmap, OnFileSavedListener listener) {
        String fileName = "FaceCamera_" + System.currentTimeMillis() + ".jpg";
        File cacheFile = new File(context.getCacheDir(), fileName);
        try (FileOutputStream outputStream = new FileOutputStream(cacheFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            listener.onFileSaved(cacheFile);

        } catch (Exception e) {
            listener.onError(e);
        }
    }

    @Nullable
    private Bitmap rotateBitmap(ImageCapture.OutputFileResults outputFileResults) {
        Uri currentUri = outputFileResults.getSavedUri();
        if (currentUri == null) return null;

        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), currentUri);
        } catch (IOException e) {
            return null;
        }

        // 이미지 회전 각도를 가져오기
        InputStream inputStream;
        try {
            inputStream = context.getContentResolver().openInputStream(currentUri);
        } catch (IOException e) {
            return null;
        }

        ExifInterface exif;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                exif = new ExifInterface(inputStream);
            } else {
                exif = new ExifInterface(currentUri.getPath());
            }
        } catch (IOException e) {
            return null;
        }

        int rotation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
        );

        // 회전 각도 설정
        int rotationInDegrees;
        switch (rotation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotationInDegrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotationInDegrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotationInDegrees = 270;
                break;
            default:
                rotationInDegrees = 0;
                break;
        }

        // 이미지 회전
        if (rotationInDegrees != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate((float) rotationInDegrees);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    private interface OnFileSavedListener {
        void onFileSaved(File file);

        void onError(Exception e);
    }
}
