package ru.hse.colorshare.coding;

import org.junit.Assert;
import org.junit.Test;

public class ColorDataFrameSupplierTest {
    @Test
    public void fromByteArrayDataFrameSupplierTest() {
        byte[] bytes = TestUtils.toBytes(0b0011_1100, 0b1010_1010);
        DataFrameSupplier supplier = new DataFrameSupplierFactory().get(bytes, 8);

        int[] colors1 = TestUtils.colorsFromBytes(new int[] {
                0b00, 0b11, 0b11, 0b00,
                0b10, 0b10, 0b10, 0b10
        });

        Assert.assertArrayEquals(colors1, supplier.get().getColors());
        Assert.assertThrows(IllegalStateException.class, supplier::get);
        supplier.setSuccess(false);
        Assert.assertArrayEquals(colors1, supplier.get().getColors());
        supplier.setSuccess(true);
        Assert.assertNull(supplier.get());
    }
}
