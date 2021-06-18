package ru.hse.colorshare.receiver.util;

public class ReceiverException extends RuntimeException {
    public ReceiverException() {
        super();
    }

    public ReceiverException(String message) {
        super(message);
    }

    public ReceiverException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReceiverException(Throwable cause) {
        super(cause);
    }

    protected ReceiverException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
