package ru.hse.colorshare.coding;

import androidx.annotation.NonNull;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public interface Encoder {

    @NonNull CodingTag getTag();

    @NonNull
    Object encode(@NonNull InputStream stream);

}
