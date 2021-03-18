package ru.hse.colorshare.coding;

import androidx.annotation.NonNull;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.BitSet;

public interface Encoder {

    @NonNull CodingTag getTag();

    @NonNull
    BitArray encode(@NonNull BitArray input);

}
