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

    abstract class AbstractUnit implements Unit {
        protected final int color, encodedValue;

        public AbstractUnit(int color, int encodedValue) {
            this.color = color;
            this.encodedValue = encodedValue;
        }

        @Override
        public int getColor() {
            return color;
        }

        @Override
        public int getEncodedValue() {
            return encodedValue;
        }
    }

    interface Creator {
        default DataFrame ofBytes(byte[] bytes, long checksum) {
            return ofBytes(bytes, 0, bytes.length, checksum);
        }

        DataFrame ofBytes(byte[] bytes, int offset, int length, long checksum);
    }

    List<Unit> getUnits();

    long getChecksum();

    default int[] getColors() {
        List<Unit> units = getUnits();
        int[] colors = new int[units.size()];
        int index = 0;
        for (Unit unit : units) {
            colors[index++] = unit.getColor();
        }
        return colors;
    }
}
