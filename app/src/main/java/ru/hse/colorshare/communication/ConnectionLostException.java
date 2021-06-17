package ru.hse.colorshare.communication;

public class ConnectionLostException extends Exception {

    public ConnectionLostException() {
    }

    public ConnectionLostException(String message) {
        super(message);
    }

    public ConnectionLostException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionLostException(Throwable cause) {
        super(cause);
    }
    
}
