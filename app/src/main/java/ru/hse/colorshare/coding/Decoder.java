package ru.hse.colorshare.coding;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;

public interface Decoder {
    @NonNull CodingTag getTag();

    @NonNull ByteArrayOutputStream decode(@NonNull ByteDataPresentation stream);
}
