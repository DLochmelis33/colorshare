package ru.hse.colorshare.coding;

import androidx.annotation.NonNull;

import ru.hse.colorshare.coding.dto.BitArray;

public interface Decoder {
    @NonNull
    CodingTag getTag();

    @NonNull
    BitArray decode(@NonNull BitArray frame);
}
