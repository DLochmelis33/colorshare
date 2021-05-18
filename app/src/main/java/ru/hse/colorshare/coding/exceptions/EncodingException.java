package ru.hse.colorshare.coding.exceptions;

public class EncodingException extends Exception {
    public EncodingException(Throwable cause) {
        super(cause);
    }

    public EncodingException(String message) {
        super(message);
    }
}
