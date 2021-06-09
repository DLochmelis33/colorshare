package ru.hse.colorshare.communication;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;

public interface Communicator extends Closeable {
    static @NonNull
    Communicator getColorShareTransmitterSideCommunicator(@NonNull Context context) {
        return new SoundCommunicator(context, "audible-7k-channel-0", "audible-7k-channel-0");
    }

    static @NonNull
    Communicator getColorShareReceiverSideCommunicator(@NonNull Context context) {
        return new SoundCommunicator(context, "audible-7k-channel-0", "audible-7k-channel-0");
    }

    void blockingSend(@NonNull byte[] toSendMessage, long blockingTimeoutInSeconds) throws IOException;

    // returns received message length in bytes
    int blockingReceive(@NonNull byte[] receivedMessageBuffer, long blockingTimeoutInSeconds) throws IOException;

    void stopWorking();
}
