package ru.hse.colorshare.util;

import org.junit.Assert;
import org.junit.Test;

import ru.hse.colorshare.coding.dto.ShortBitArray;

public class ShortBitArrayTest {
    @Test
    public void testFromByteArray() {
        byte[] arr;
        arr = new byte[] {-12, 109, 11};
        Assert.assertArrayEquals(arr, ShortBitArray.valueOf(arr).toByteArray());
        arr = new byte[] {-128, 127, 11, 11};
        Assert.assertArrayEquals(arr, ShortBitArray.valueOf(arr).toByteArray());
    }
}
