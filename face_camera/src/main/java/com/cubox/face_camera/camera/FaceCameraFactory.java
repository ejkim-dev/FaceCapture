package com.cubox.face_camera.camera;


import android.app.Activity;

import com.cubox.face_camera.camera.view.FaceCameraImpl;

public class FaceCameraFactory {
    public static FaceCamera create(Activity activity) {
        return new FaceCameraImpl(activity);
    }
}
