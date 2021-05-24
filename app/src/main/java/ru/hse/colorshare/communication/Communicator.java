package ru.hse.colorshare.communication;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

public interface Communicator {
    static @NonNull
    Communicator getColorShareTransmitterSideCommunicator(@NonNull Context context, long blockingTimeoutInSeconds) {
        return new SoundCommunicator(context, "audible-7k-channel-0", "audible-7k-channel-0", blockingTimeoutInSeconds);
    }

    static @NonNull
    Communicator getColorShareReceiverSideCommunicator(@NonNull Context context, long blockingTimeoutInSeconds) {
        return new SoundCommunicator(context, "audible-7k-channel-0", "audible-7k-channel-0", blockingTimeoutInSeconds);
    }

    void blockingSend(@NonNull byte[] toSendMessage) throws IOException;

    void blockingReceive(@NonNull byte[] receivedMessageBuffer) throws IOException;

}
