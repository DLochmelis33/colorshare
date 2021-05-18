package ru.hse.colorshare.generator.preprocessor;

import java.io.IOException;
import java.nio.ByteBuffer;

/*
    Здесь возможна предобработка данных,
    которые мы пересылаем, например перед тем,
    как давать кодировщику в цвета - можно сжать данные или применить алгоритмы кодирования с восстановлением ошибок.
 */

public interface EncodingPreprocessor {

    int readBytes(ByteBuffer buffer, int length) throws IOException;

    long left() throws IOException;
}
