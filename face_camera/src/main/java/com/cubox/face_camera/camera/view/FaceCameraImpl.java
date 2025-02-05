package com.cubox.face_camera.camera.view;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.cubox.face_camera.camera.FaceCamera;
import com.cubox.face_camera.camera.impl.CircleOverlayView;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;


 public class FaceCameraImpl implements FaceCamera {
    private Context context;
    private PreviewView previewView;
    private Preview preview;
    private ProcessCameraProvider provider;
    private ImageCapture imageCapture;

    public FaceCameraImpl(Activity activity) {
        FrameLayout rootLayout = activity.findViewById(android.R.id.content);
        CircleOverlayView circleOverlayView = new CircleOverlayView(activity);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        rootLayout.addView(circleOverlayView, params);

        this.context = activity;
    }

    @Override
    public void initialize() {
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
    public void takePictureWithDelay(onImageSavedListener listener, int delayMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            takePicture(listener);
        }, delayMillis);
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
                                listener.onError("사진 저장 실패 : "+e.getMessage());
                            }
                        });

                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        listener.onError("사진 저장 실패 : "+exception.getMessage());
                    }
                });
    }

    @Override
    public PreviewView getPreviewView() {
        return previewView;
    }

     @Override
     public void saveFileToGallery(File cacheFile, onImageSavedListener listener) {
         if (cacheFile == null || !cacheFile.exists()) {
                listener.onError("파일을 찾을 수 없습니다.");
             return;
         }

         String fileName = cacheFile.getName();
         OutputStream fos = null;
         try {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                 // Android Q 이상에서는 MediaStore API를 사용해 갤러리에 저장
                 ContentResolver resolver = context.getContentResolver();
                 ContentValues contentValues = new ContentValues();
                 contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                 // 파일의 MIME 타입은 파일에 맞게 수정 (예: image/jpeg, image/png 등)
                 contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                 contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FaceCamera");

                 Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                 if (imageUri != null) {
                     fos = resolver.openOutputStream(imageUri);
                 }
             } else {
                 // Android Q 미만에서는 외부 저장소의 Pictures 디렉토리에 파일 직접 저장 후 미디어 스캐너 호출
                 File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                 File appDir = new File(picturesDir, "FaceCamera");
                 if (!appDir.exists()) {
                     appDir.mkdirs();
                 }
                 File destFile = new File(appDir, fileName);
                 fos = new FileOutputStream(destFile);
             }

             if (fos != null) {
                 // 캐시 파일의 내용을 읽어서 출력 스트림에 기록
                 FileInputStream fis = new FileInputStream(cacheFile);
                 byte[] buffer = new byte[1024];
                 int len;
                 while ((len = fis.read(buffer)) != -1) {
                     fos.write(buffer, 0, len);
                 }
                 fis.close();
                 fos.flush();
                 fos.close();

                 listener.onImageSavedToGallery("갤러리에 저장되었습니다.");

                 if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                     // Android Q 미만에서는 갤러리에 바로 반영되도록 미디어 스캐너 호출
                     File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                     File appDir = new File(picturesDir, "FaceCamera");
                     File destFile = new File(appDir, fileName);
                     Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                     Uri contentUri = Uri.fromFile(destFile);
                     mediaScanIntent.setData(contentUri);
                     context.sendBroadcast(mediaScanIntent);
                 }
             }
         } catch (IOException e) {
             listener.onError("갤러리에 저장 실패 : "+e.getMessage());
         }
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
