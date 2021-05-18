package ru.hse.colorshare.receiver;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBulkDecoder implements BulkDecoder {
    private enum State {
        WAITING_NEW_BULK,
        ENCODING_BULK
    }

    private State state;

    protected final Map<Long, Integer> checksumToIndex = new HashMap<>();
    protected final BitSet readyFrames = new BitSet();

    protected byte[][] readFrames;

    protected abstract void flushFrames() throws IOException;

    @Override
    public void setBulkChecksums(long[] checksums) {
        if (state != State.WAITING_NEW_BULK) {
            throw new IllegalStateException("Need to finish encoding before");
        }
        checksumToIndex.clear();
        readyFrames.clear();
        for (int i = 0; i < checksums.length; i++) {
            checksumToIndex.put(checksums[i], i);
        }
        readFrames = new byte[checksums.length][];
        state = State.ENCODING_BULK;
    }

    @Override
    public boolean isBulkEncoded() {

        return readyFrames.nextClearBit(0) >= checksumToIndex.size();
    }

    protected boolean testChecksum(long checksum) {
        return checksumToIndex.containsKey(checksum);
    }

    protected void testSucceed(int assumedIndex) throws IOException {
        readyFrames.set(assumedIndex);
        if (isBulkEncoded()) {
            flushFrames();
            state = State.WAITING_NEW_BULK;
        }
    }
}
