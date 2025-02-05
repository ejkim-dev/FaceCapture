package com.example.facecapture.camera;

public class FaceCameraFactory {
    public static FaceCamera create() {
        return new FaceCameraImpl();
    }
}
