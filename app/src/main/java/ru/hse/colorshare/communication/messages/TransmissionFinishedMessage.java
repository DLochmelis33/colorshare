package ru.hse.colorshare.communication.messages;

import ru.hse.colorshare.communication.Message;

public class TransmissionFinishedMessage extends Message {

    protected TransmissionFinishedMessage() {
        super(MessageType.TRANSMISSION_FINISHED);
    }

    @Override
    protected byte[] toByteArray() {
        return new byte[0];
    }

    public static Message parseDerivedFrom(byte[] byteArray) {
        return new TransmissionFinishedMessage();
    }
}
