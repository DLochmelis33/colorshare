package ru.hse.colorshare.coding.dto;

import java.util.List;

/*
    Description of a data structure which is transferring to VIEW
 */

public interface DataFrame {

    interface Unit {
        int getColor();
        int getEncodedValue();
    }

    List<Unit> getUnits();
}
