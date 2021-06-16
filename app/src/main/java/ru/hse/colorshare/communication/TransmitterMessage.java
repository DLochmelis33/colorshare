package ru.hse.colorshare.communication;

public class TransmitterMessage {
    private final MessageType messageType;
    private final int bulkIndex;
    private final long[] bulkChecksums;
    private final int gridRows;
    private final int gridCols;

    public TransmitterMessage(MessageType messageType, int bulkIndex, long[] bulkChecksums, int gridRows, int gridCols) {
        this.messageType = messageType;
        this.bulkIndex = bulkIndex;
        this.bulkChecksums = bulkChecksums;
        this.gridRows = gridRows;
        this.gridCols = gridCols;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public int getBulkIndex() {
        return bulkIndex;
    }

    public long[] getBulkChecksums() {
        return bulkChecksums;
    }

    public int getGridRows() {
        return gridRows;
    }

    public int getGridCols() {
        return gridCols;
    }


    public enum MessageType {
        BULK_INFO,
        TRANSMISSION_SUCCEED,
        TRANSMISSION_CANCELED
    }
}
