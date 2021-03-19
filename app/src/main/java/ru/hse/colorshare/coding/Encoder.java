package ru.hse.colorshare.coding;

import androidx.annotation.NonNull;

import ru.hse.colorshare.coding.dto.BitArray;

public interface Encoder {
    @NonNull
    CodingTag getTag();

    @NonNull
    BitArray encode(@NonNull BitArray frame);
}
