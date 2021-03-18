package ru.hse.colorshare.coding.algorithms.hamming;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.InputStream;
import java.util.BitSet;

import ru.hse.colorshare.coding.BitArray;
import ru.hse.colorshare.coding.CodingTag;
import ru.hse.colorshare.coding.Encoder;

public class HammingEncoder implements Encoder {
    private static final int SOURCE_FRAME_SIZE_DEFAULT = 15;

    private final int sourceFrameSize;
    private final int controlBits;

    public HammingEncoder() {
        this(SOURCE_FRAME_SIZE_DEFAULT);
    }

    public HammingEncoder(int sourceFrameSize) {
        this.sourceFrameSize = sourceFrameSize;
        this.controlBits = Util.calculateControlBits(sourceFrameSize);
    }

    @NonNull
    @Override
    public CodingTag getTag() {
        return CodingTag.HAMMING;
    }

    @NonNull
    @Override
    public BitArray encode(@NonNull BitArray input) {
        boolean[] control = Util.calculateControlBits(input.data, controlBits);
        BitArray resulting = new BitArray(sourceFrameSize + controlBits);
        Util.ofNonControl(sourceFrameSize).forEach(
                e -> resulting.data.set(e.actual, input.data.get(e.count))
        );
        Util.ofControl(controlBits).forEach(
                e -> resulting.data.set(e.actual, control[e.count])
        );
        return resulting;
    }
}
