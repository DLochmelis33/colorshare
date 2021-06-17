package ru.hse.colorshare.communication.messages;

import java.nio.ByteBuffer;

import ru.hse.colorshare.communication.Message;

public class TransmissionCanceledMessage extends Message {

    public TransmissionCanceledMessage() {
        super(MessageType.TRANSMISSION_CANCELED);
    }

    protected byte[] toByteArray() {
        return new byte[0];
    }

    public static Message parseDerivedFrom(ByteBuffer byteBuffer) {
        return new TransmissionCanceledMessage();
    }
}
