package ru.hse.colorshare.coding.algorithms.hamming;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.InputStream;
import java.util.BitSet;

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
    public Object encode(@NonNull InputStream stream) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private BitSet encodeFrame(BitSet input) {
        boolean[] control = Util.calculateControlBits(input, controlBits);
        BitSet resulting = new BitSet(sourceFrameSize + controlBits);
        Util.ofNonControl(sourceFrameSize).forEach(
                e -> resulting.set(e.actual, input.get(e.count))
        );
        Util.ofControl(controlBits).forEach(
                e -> resulting.set(e.actual, control[e.count])
        );
        return resulting;
    }
}
