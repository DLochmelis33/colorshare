package ru.hse.colorshare.communication;

import java.util.concurrent.TimeoutException;

import ru.hse.colorshare.communication.impl.SoniColorShareReceiverCommunicator;

public interface ColorShareReceiverCommunicator extends Communicator {

    static ColorShareReceiverCommunicator getInstance() {
        return new SoniColorShareReceiverCommunicator();
    }

    void sendBulkReceivedMessage();

    void sendTransmissionCancelMessage();

    TransmitterMessage waitForMessage(int timeoutSeconds) throws TimeoutException;

}
