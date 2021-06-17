package ru.hse.colorshare.communication.messages;

import java.nio.ByteBuffer;

import ru.hse.colorshare.communication.Message;

public class PairingMessage extends Message {

    public PairingMessage() {
        super(MessageType.PAIRING);
    }

    @Override
    protected byte[] toByteArray() {
        return new byte[0];
    }

    public static Message parseDerivedFrom(ByteBuffer byteBuffer) {
        return new PairingMessage();
    }
}
