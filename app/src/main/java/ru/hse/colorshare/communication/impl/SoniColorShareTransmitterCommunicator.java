package ru.hse.colorshare.communication.impl;

import java.util.concurrent.TimeoutException;

import ru.hse.colorshare.communication.ColorShareTransmitterCommunicator;
import ru.hse.colorshare.communication.ReceiverMessage;
import ru.hse.colorshare.transmitter.TransmissionParams;

public class SoniColorShareTransmitterCommunicator extends SoniCommunicator implements ColorShareTransmitterCommunicator {
    @Override
    public void sendBulkMessage(int bulkIndex, long[] bulkChecksums, TransmissionParams transmissionParams) {

    }

    @Override
    public void sendTransmissionSuccessfullyFinishedMessage() {

    }

    @Override
    public void sendTransmissionCanceledMessage() {

    }

    @Override
    public ReceiverMessage waitForMessage(int timeoutSeconds) throws TimeoutException {
        return null;
    }
}
