package ru.hse.colorshare.communication.messages;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ru.hse.colorshare.communication.Message;

public class BulkMessage extends Message {
    private final long[] bulkChecksums;
    private final int gridRows;
    private final int gridCols;

    public BulkMessage(long[] bulkChecksums, int gridRows, int gridCols) {
        super(MessageType.BULK);
        this.bulkChecksums = bulkChecksums;
        this.gridRows = gridRows;
        this.gridCols = gridCols;
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

    @Override
    protected byte[] toByteArray() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getSizeInBytes());
        byteBuffer.putInt(gridRows);
        byteBuffer.putInt(gridCols);
        for (long checksum : bulkChecksums) {
            byteBuffer.putLong(checksum);
        }
        return byteBuffer.array();
    }

    public static Message parseDerivedFrom(ByteBuffer byteBuffer) {
        int gridRows = byteBuffer.getInt();
        int gridCols = byteBuffer.getInt();
        List<Long> checksumsList = new ArrayList<>();
        while (byteBuffer.remaining() >= Long.SIZE / 8) {
            checksumsList.add(byteBuffer.getLong());
        }
        if (byteBuffer.hasRemaining()) {
            throw new IllegalStateException("Parse failed");
        }
        long[] checksums = new long[checksumsList.size()];
        for (int i = 0; i < checksumsList.size(); i++) {
            checksums[i] = checksumsList.get(i);
        }
        return new BulkMessage(checksums, gridRows, gridCols);
    }

    private int getSizeInBytes() {
        return 3 * Integer.SIZE / 8 + bulkChecksums.length * Long.SIZE / 8;
    }
}
