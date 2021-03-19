package ru.hse.colorshare.coding;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Queue;

import ru.hse.colorshare.util.Function;
import ru.hse.colorshare.util.Generator;

public final class ColorFrameGenerator implements Generator<DataFrame> {

    private static final int TO_READ_SIZE_DEFAULT = 1024;
    private static final int TO_ENCODE_SIZE_DEFAULT = 4;

    private final int encodeSize;

    private final BufferedInputStream stream;
    private final byte[] toRead;
    private boolean isAvailable = false;

    private final Encoder encoder;

    private final Queue<DataFrame> queue;

    private final Function<BitArray, ? extends DataFrame> mapper;

    public ColorFrameGenerator(InputStream stream, Encoder encoder, Function<BitArray, ? extends DataFrame> mapper) {
        this(stream, encoder, mapper, TO_READ_SIZE_DEFAULT, TO_ENCODE_SIZE_DEFAULT);
    }

    public ColorFrameGenerator(InputStream stream, Encoder encoder, Function<BitArray, ? extends DataFrame> mapper, int toReadSize, int toEncodeSize) {
        this.encoder = encoder;
        this.stream = new BufferedInputStream(stream);
        this.queue = new ArrayDeque<>();
        this.encodeSize = toEncodeSize;
        this.toRead = new byte[toReadSize];
        this.mapper = mapper;
    }


    private void readStream() throws IOException {
        int actuallyRed = stream.read(toRead);
        for (int i = 0; i < actuallyRed; i++) {
            queue.add(
                    mapper.apply(encoder.encode(new BitArray(toRead, i, encodeSize)))
            );
        }
        isAvailable = stream.available() > 0;
    }

    @Override
    public boolean hasMore() {
        return queue.isEmpty() && !isAvailable;
    }

    @Override
    public DataFrame get() {
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
