package ru.hse.colorshare.communication.messages;

import ru.hse.colorshare.communication.Message;

public class TransmissionCanceledMessage extends Message {

    protected TransmissionCanceledMessage() {
        super(MessageType.TRANSMISSION_CANCELED);
    }

    protected byte[] toByteArray() {
        return new byte[0];
    }

    public static Message parseDerivedFrom(byte[] byteArray) {
        return new TransmissionCanceledMessage();
    }
}
