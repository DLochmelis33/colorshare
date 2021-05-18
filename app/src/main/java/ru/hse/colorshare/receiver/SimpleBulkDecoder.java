package ru.hse.colorshare.receiver;

import java.io.IOException;
import java.io.OutputStream;

public class SimpleBulkDecoder extends AbstractBulkDecoder {

    private final OutputStream stream;

    public SimpleBulkDecoder(OutputStream stream) {
        this.stream = stream;
    }

    @Override
    protected void flushFrames() throws IOException {
        for (byte[] bytes : readFrames) {
            stream.write(bytes);
        }
    }

    @Override
    public void testFrame(int[] colors) {

    }

    @Override
    public void setReceivingParameters() {
        // TODO
    }
}
