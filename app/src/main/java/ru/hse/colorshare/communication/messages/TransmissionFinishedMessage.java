package ru.hse.colorshare.communication.messages;

import java.nio.ByteBuffer;

import ru.hse.colorshare.communication.Message;

public class TransmissionFinishedMessage extends Message {

    public TransmissionFinishedMessage() {
        super(MessageType.TRANSMISSION_FINISHED);
    }

    @Override
    protected byte[] toByteArray() {
        return new byte[0];
    }

    public static Message parseDerivedFrom(ByteBuffer byteBuffer) {
        return new TransmissionFinishedMessage();
    }
}
