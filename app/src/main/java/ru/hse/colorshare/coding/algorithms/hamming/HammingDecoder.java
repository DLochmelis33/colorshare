package ru.hse.colorshare.coding.algorithms.hamming;

import androidx.annotation.NonNull;

import ru.hse.colorshare.coding.CodingTag;
import ru.hse.colorshare.coding.Decoder;

public class HammingDecoder implements Decoder {

    @NonNull
    @Override
    public CodingTag getTag() {
        return CodingTag.HAMMING;
    }

    @NonNull
    @Override
    public BitArray decode(@NonNull BitArray input) {
        CodingProperties properties = CodingProperties.ofEncodedSize(input.length);
        int positionOfError = HammingUtils.calculateSyndrome(input, properties.controlBits).toInteger() - 1;
        if (positionOfError != -1) {
            input.flip(positionOfError);
        }
        BitArray resulting = new BitArray(properties.sourceSize);
        HammingUtils.ofNonControl(properties.sourceSize).forEach(
                e -> resulting.set(e.index, input.get(e.value))
        );
        return resulting;
    }

}
