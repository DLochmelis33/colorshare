package ru.hse.colorshare.communication.messages;

import java.nio.ByteBuffer;

import ru.hse.colorshare.communication.Message;

public class BulkReceivedMessage extends Message {

    public BulkReceivedMessage() {
        super(MessageType.BULK_RECEIVED);
    }

    @Override
    protected byte[] toByteArray() {
        return new byte[0];
    }

    public static Message parseDerivedFrom(ByteBuffer byteBuffer) {
        return new BulkReceivedMessage();
    }
}
