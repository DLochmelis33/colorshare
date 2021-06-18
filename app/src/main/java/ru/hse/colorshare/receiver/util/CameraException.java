package ru.hse.colorshare.receiver.util;

public class CameraException extends IllegalStateException {
    public CameraException() {
        super();
    }

    public CameraException(String s) {
        super(s);
    }

    public CameraException(String message, Throwable cause) {
        super(message, cause);
    }

    public CameraException(Throwable cause) {
        super(cause);
    }
}
