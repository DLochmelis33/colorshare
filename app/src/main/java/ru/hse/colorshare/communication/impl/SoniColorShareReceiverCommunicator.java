package ru.hse.colorshare.communication.impl;

import java.util.concurrent.TimeoutException;

import ru.hse.colorshare.communication.ColorShareReceiverCommunicator;
import ru.hse.colorshare.communication.TransmitterMessage;

public class SoniColorShareReceiverCommunicator extends SoniCommunicator implements ColorShareReceiverCommunicator {
    @Override
    public void sendBulkReceivedMessage() {

    }

    @Override
    public void sendTransmissionCancelMessage() {

    }

    @Override
    public TransmitterMessage waitForMessage(int timeoutSeconds) throws TimeoutException {
        return null;
    }
}
