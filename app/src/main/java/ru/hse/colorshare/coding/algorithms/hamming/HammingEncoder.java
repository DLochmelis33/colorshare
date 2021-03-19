package ru.hse.colorshare.coding.algorithms.hamming;

import androidx.annotation.NonNull;

import ru.hse.colorshare.coding.BitArray;
import ru.hse.colorshare.coding.CodingTag;
import ru.hse.colorshare.coding.Encoder;

public class HammingEncoder implements Encoder {
    @NonNull
    @Override
    public CodingTag getTag() {
        return CodingTag.HAMMING;
    }

    @NonNull
    @Override
    public BitArray encode(@NonNull BitArray input) {
        int sourceFrameSize = input.length, controlBits = HammingUtils.calculateControlBits(sourceFrameSize);
        boolean[] control = HammingUtils.calculateControlBits(input.data, controlBits);
        BitArray resulting = new BitArray(sourceFrameSize + controlBits);
        HammingUtils.ofNonControl(sourceFrameSize).forEach(
                e -> resulting.data.set(e.actual, input.data.get(e.count))
        );
        HammingUtils.ofControl(controlBits).forEach(
                e -> resulting.data.set(e.actual, control[e.count])
        );
        return resulting;
    }
}
