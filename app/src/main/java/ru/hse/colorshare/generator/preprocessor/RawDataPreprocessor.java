package ru.hse.colorshare.generator.preprocessor;

import java.io.IOException;

/*
    Здесь возможна предобработка данных,
    которые мы пересылаем, например перед тем,
    как давать кодировщику в цвета - можно сжать данные или применить алгоритмы кодирования с восстановлением ошибок.
 */

public interface RawDataPreprocessor {

    default int getBytes(byte[] buffer) throws IOException {
        return getBytes(buffer, 0, buffer.length);
    }

    int getBytes(byte[] buffer, int offset, int length) throws IOException;

    void returnBytes(int count) throws IOException;

    long left() throws IOException;
}
