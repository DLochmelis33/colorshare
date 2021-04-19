package ru.hse.colorshare.coding;

import androidx.annotation.NonNull;

import ru.hse.colorshare.coding.algorithms.hamming.BitArray;

public interface Decoder {
    @NonNull
    CodingTag getTag();

    @NonNull
    BitArray decode(@NonNull BitArray frame);
}
