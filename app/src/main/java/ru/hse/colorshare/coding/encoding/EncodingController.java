package ru.hse.colorshare.coding.encoding;

import android.content.Context;
import android.net.Uri;

import java.io.Closeable;
import java.io.FileNotFoundException;

import ru.hse.colorshare.coding.encoding.impl.SimpleEncodingController;
import ru.hse.colorshare.coding.exceptions.EncodingException;
import ru.hse.colorshare.transmitter.TransmissionParams;

/*
   Основной интерфейс описывающий логику кодирования байтов в цвета
 */

public interface EncodingController extends Closeable {

    static EncodingController create(Uri filename, Context context) throws FileNotFoundException {
        return new SimpleEncodingController(filename, context);
    }

    // Возвращается следующий балк дата фрэймов. null если данные кончились
    DataFrameBulk getNextBulk() throws EncodingException;

    // Возвращается примерное количество осавшихся байт
    long estimateSize() throws EncodingException;

    // Возвращает текущий индекс балка
    int getBulkIndex();

    // Информация для логирования
    String getInfo();

    // Установка параметров передачи
    void setTransmissionParameters(int unitsPerFrame, int framesPerBulk);

    void setTransmissionParameters(TransmissionParams params);
}
