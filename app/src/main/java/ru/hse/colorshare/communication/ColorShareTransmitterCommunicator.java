package ru.hse.colorshare.communication;

import java.util.concurrent.TimeoutException;

import ru.hse.colorshare.communication.impl.SoniColorShareTransmitterCommunicator;
import ru.hse.colorshare.transmitter.TransmissionParams;

public interface ColorShareTransmitterCommunicator extends Communicator {

    static ColorShareTransmitterCommunicator getInstance() {
        return new SoniColorShareTransmitterCommunicator();
    }

    void sendBulkMessage(int bulkIndex, long[] bulkChecksums, TransmissionParams transmissionParams);

    void sendTransmissionSuccessfullyFinishedMessage();

    void sendTransmissionCanceledMessage();

    ReceiverMessage waitForMessage(int timeoutSeconds) throws TimeoutException;

}
