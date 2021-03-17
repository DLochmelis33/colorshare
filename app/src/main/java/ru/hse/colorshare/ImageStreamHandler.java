package ru.hse.colorshare;

import android.media.Image;

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

    private ConcurrentLinkedQueue<Image> imageQueue;

    public void addImage() {
        // ! TODO
    }

    public void nextImage() {
        // ! TODO
    }

}
