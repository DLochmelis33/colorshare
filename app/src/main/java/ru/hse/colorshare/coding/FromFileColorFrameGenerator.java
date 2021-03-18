package ru.hse.colorshare.coding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import ru.hse.colorshare.util.Generator;

public final class FromFileColorFrameGenerator implements Generator<ColorFrame> {

    private static final int TO_READ_SIZE = 1024;
    private static final int TO_ENCODE_SIZE = 1;

    private BufferedInputStream stream;
    private byte[] toRead = new byte[TO_READ_SIZE];
    private boolean isAvailable = false;

    private Encoder encoder;

    private final Queue<ColorFrame> queue;

    public FromFileColorFrameGenerator(InputStream stream, Encoder encoder) {
        this.encoder = encoder;
        this.stream = new BufferedInputStream(stream);
        this.queue = new ArrayDeque<>();
    }

    private void readStream() throws IOException {
        int actuallyRed = stream.read(toRead);
        for (int i = 0; i < actuallyRed; i++) {
            queue.add(
                    ColorFrame.ofBits(encoder.encode(new BitArray(toRead, i, TO_ENCODE_SIZE)))
            );
        }
        isAvailable = stream.available() > 0;
    }

    @Override
    public boolean hasMore() {
        return queue.isEmpty() && !isAvailable;
    }

    @Override
    public ColorFrame get() {
        if (queue.isEmpty() && isAvailable) {
            try {
                readStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return queue.poll();
        }
        throw new IllegalStateException();
    }

}
