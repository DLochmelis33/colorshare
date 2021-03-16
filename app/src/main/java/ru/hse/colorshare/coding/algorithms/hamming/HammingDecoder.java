package ru.hse.colorshare.coding.algorithms.hamming;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;

import ru.hse.colorshare.coding.ByteDataPresentation;
import ru.hse.colorshare.coding.CodingTag;
import ru.hse.colorshare.coding.Decoder;

public class HammingDecoder implements Decoder {
    private final int sourceFrameSize;
    private final int controlBits;

    public HammingDecoder(int sourceFrameSize) {
        this.controlBits = Util.calculateControlBits(sourceFrameSize);
        this.sourceFrameSize = sourceFrameSize + this.controlBits;
    }

    @NonNull
    @Override
    public CodingTag getTag() {
        return CodingTag.HAMMING;
    }

    @NonNull
    @Override
    public ByteArrayOutputStream decode(@NonNull ByteDataPresentation stream) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private BitSet decodeFrame(BitSet input) {
        int positionOfError = Util.fromBoolean(Util.calculateSyndrome(input, controlBits)) - 1;
        if (positionOfError != -1) {
            input.flip(positionOfError);
        }
        BitSet resulting = new BitSet(sourceFrameSize);
        Util.ofNonControl(sourceFrameSize).forEach(
                e -> resulting.set(e.count, input.get(e.actual))
        );
        return resulting;
    }
}
