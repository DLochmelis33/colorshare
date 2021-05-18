package ru.hse.colorshare.coding.decoding.impl;

import ru.hse.colorshare.coding.decoding.DecodingController;

public class SimpleDecodingController implements DecodingController {
    @Override
    public void setBulkChecksums(long[] checksums) {

    }

    @Override
    public boolean isBulkEncoded() {
        return false;
    }

    @Override
    public void testFrame(int[] colors) {

    }

    @Override
    public void setReceivingParameters() {

    }
}
