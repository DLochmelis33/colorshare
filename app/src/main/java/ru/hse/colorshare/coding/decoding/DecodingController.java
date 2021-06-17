package ru.hse.colorshare.coding.decoding;

import android.content.Context;
import android.net.Uri;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import ru.hse.colorshare.coding.decoding.impl.SimpleDecodingController;
import ru.hse.colorshare.coding.exceptions.DecodingException;

public interface DecodingController extends Closeable {

    static DecodingController create(Uri filename, Context context) throws FileNotFoundException {
        return create(context.getContentResolver().openOutputStream(filename));
    }

    static DecodingController create(OutputStream stream) {
        return new SimpleDecodingController(stream);
    }

    void startNewBulkEncoding(long[] checksums);

    boolean isBulkFullyEncoded();

    void testFrame(int[] colors);

    void setReceivingParameters(/* пока не знаю, но что-то сюда надо */);

    void flush() throws IOException;
}
