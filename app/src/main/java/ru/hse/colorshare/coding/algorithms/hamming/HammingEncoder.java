package ru.hse.colorshare.coding.algorithms.hamming;

import androidx.annotation.NonNull;

import ru.hse.colorshare.coding.CodingTag;
import ru.hse.colorshare.coding.Encoder;

public class HammingEncoder implements Encoder {
    @NonNull
    @Override
    public CodingTag getTag() {
        return CodingTag.HAMMING;
    }

    @NonNull
    public BitArray encode(@NonNull BitArray input) {
        CodingProperties properties = CodingProperties.ofSourceSize(input.length);
        ShortBitArray control = HammingUtils.calculateControlBits(input, properties.controlBits);
        BitArray resulting = new BitArray(properties.sourceSize + properties.controlBits);
        HammingUtils.ofNonControl(properties.sourceSize).forEach(
                e -> resulting.set(e.value, input.get(e.index))
        );
        HammingUtils.ofControl(properties.controlBits).forEach(
                e -> resulting.set(e.value, control.get(e.index))
        );
        return resulting;
    }
}
