package ru.hse.colorshare.communication.messages;

import ru.hse.colorshare.communication.Message;

public class BulkReceivedMessage extends Message {

    protected BulkReceivedMessage() {
        super(MessageType.BULK_RECEIVED);
    }

    @Override
    protected byte[] toByteArray() {
        return new byte[0];
    }

    public static Message parseDerivedFrom(byte[] byteArray) {
        return new BulkReceivedMessage();
    }
}
