package ru.hse.colorshare.coding;

import java.util.List;

public interface DataFrame {
    interface Unit {
        int getColor();
        int getEncodedValue();
    }

    List<Unit> getUnits();
}
