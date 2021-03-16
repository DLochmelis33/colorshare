package ru.hse.colorshare.coding.algorithms.hamming;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.InputStream;
import java.util.BitSet;

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
    public Object encode(@NonNull InputStream stream) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public BitSet encodeFrame(BitSet input) {
        int inputLength = 15, countOfControlBits = 5;
        boolean[] controlBits = Util.calculateControlBits(input, countOfControlBits);
        BitSet resulting = new BitSet(inputLength + countOfControlBits);
        Util.ofNonControl(inputLength).forEach(
                e -> resulting.set(e.actual, input.get(e.count))
        );
        Util.ofControl(countOfControlBits).forEach(
                e -> resulting.set(e.actual, controlBits[e.count])
        );
        return resulting;
    }
}
