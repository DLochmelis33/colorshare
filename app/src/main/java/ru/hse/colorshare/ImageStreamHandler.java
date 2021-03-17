package ru.hse.colorshare;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ImageStreamHandler {
    // ! TODO

    private static ImageStreamHandler instance = null;

    public static ImageStreamHandler getInstance() {
        if (instance == null) {
            instance = new ImageStreamHandler();
        }
        return instance;
    }

    private ImageStreamHandler() {

    }

    private ConcurrentLinkedQueue<?> imageQueue;

    public void addImage() {
        // ! TODO
    }

    public void nextImage() {
        // ! TODO
    }

}
