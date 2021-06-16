package ru.hse.colorshare.communication;

public class ReceiverMessage {

    private final MessageType messageType;

    public ReceiverMessage(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public enum MessageType {
        BULK_RECEIVED,
        TRANSMISSION_CANCELED
    }
}
