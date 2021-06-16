package ru.hse.colorshare.communication.impl;

import java.util.concurrent.TimeoutException;

import ru.hse.colorshare.communication.Communicator;

public class SoniCommunicator implements Communicator {
    @Override
    public long getCommunicatorId() {
        return 0;
    }

    @Override
    public void bindPartnerId(long partnerId) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void sendPairingMessage() {

    }

    @Override
    public long waitForPairingMessage(int timeoutSeconds) throws TimeoutException {
        return 0;
    }
}
