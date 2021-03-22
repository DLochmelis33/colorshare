package ru.hse.colorshare.coding;

import java.util.zip.Checksum;

import ru.hse.colorshare.coding.dto.ThreeBitColorFrame;

public final class DataFrameSupplierFactory {
    DataFrameSupplier get(DataFrameCreationParameters parameters) {
        byte[] bytes = new byte[]{1, 23, 100, 34};
        return new ColorFrameSupplier(
                bytes,
                new Checksum() {
                    @Override
                    public void update(int b) {

                    }

                    @Override
                    public void update(byte[] b, int off, int len) {

                    }

                    @Override
                    public long getValue() {
                        return 0;
                    }

                    @Override
                    public void reset() {

                    }
                },
                ThreeBitColorFrame::valueOf,
                ThreeBitColorFrame.estimateByteSize(parameters.getUnitsPerFrame()));
    }
}
