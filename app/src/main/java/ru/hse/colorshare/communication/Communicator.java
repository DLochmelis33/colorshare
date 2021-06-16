package ru.hse.colorshare.communication;

import java.util.concurrent.TimeoutException;

public interface Communicator {

    long getCommunicatorId();

    void bindPartnerId(long partnerId);

    void shutdown();

    // pairing

    void sendPairingRequest();

    long waitForPairingRequest(int timeoutSeconds) throws TimeoutException;

    void sendPairingSucceedMessage();

    void waitForPairingSucceedMessage(int timeoutSeconds) throws TimeoutException;

}
