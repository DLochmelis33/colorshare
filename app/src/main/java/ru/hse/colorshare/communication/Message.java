package ru.hse.colorshare.communication;

import java.nio.ByteBuffer;

import ru.hse.colorshare.communication.messages.BulkMessage;
import ru.hse.colorshare.communication.messages.BulkReceivedMessage;
import ru.hse.colorshare.communication.messages.PairingMessage;
import ru.hse.colorshare.communication.messages.TransmissionCanceledMessage;
import ru.hse.colorshare.communication.messages.TransmissionFinishedMessage;

public abstract class Message {

    private int id = -1;
    private long uniqueSenderId;
    private final MessageType messageType;

    protected Message(MessageType messageType) {
        this.messageType = messageType;
    }

    public final int getId() {
        return id;
    }

    public final void setIds(int id, long uniqueSenderId) {
        this.id = id;
        this.uniqueSenderId = uniqueSenderId;
    }

    public final long getSenderId() {
        return uniqueSenderId;
    }

    public final MessageType getMessageType() {
        return messageType;
    }

    public static byte[] toByteArray(Message message) {
        byte[] messageBytes = message.toByteArray();
        ByteBuffer byteBuffer = ByteBuffer.allocate(getSizeInBytes() + messageBytes.length);
        byteBuffer.putInt(message.getId());
        byteBuffer.putLong(message.getSenderId());
        byteBuffer.putInt(message.getMessageType().ordinal());
        byteBuffer.put(messageBytes);
        return byteBuffer.array();
    }

    public static Message parseFrom(byte[] byteArray) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray, 0, byteArray.length);
        int id = byteBuffer.getInt();
        long senderId = byteBuffer.getLong();
        MessageType messageType = MessageType.values()[byteBuffer.getInt()];
        Message parsedMessage;
        switch (messageType) {
            case PAIRING:
                parsedMessage = PairingMessage.parseDerivedFrom(byteBuffer);
                break;
            case BULK:
                parsedMessage = BulkMessage.parseDerivedFrom(byteBuffer);
                break;
            case BULK_RECEIVED:
                parsedMessage = BulkReceivedMessage.parseDerivedFrom(byteBuffer);
                break;
            case TRANSMISSION_FINISHED:
                parsedMessage = TransmissionFinishedMessage.parseDerivedFrom(byteBuffer);
                break;
            case TRANSMISSION_CANCELED:
                parsedMessage = TransmissionCanceledMessage.parseDerivedFrom(byteBuffer);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + messageType);
        }
        parsedMessage.setIds(id, senderId);
        return parsedMessage;
    }

    protected abstract byte[] toByteArray();

    private static int getSizeInBytes() {
        return 2 * Integer.SIZE / 8 + Long.SIZE / 8;
    }

    public enum MessageType {
        PAIRING,
        BULK,
        BULK_RECEIVED,
        TRANSMISSION_FINISHED,
        TRANSMISSION_CANCELED
    }

}
