package ru.hse.colorshare.communication;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import org.quietmodem.Quiet.FrameReceiver;
import org.quietmodem.Quiet.FrameReceiverConfig;
import org.quietmodem.Quiet.FrameTransmitter;
import org.quietmodem.Quiet.FrameTransmitterConfig;
import org.quietmodem.Quiet.ModemException;

import java.io.IOException;

public class SoundCommunicator implements Communicator {

    final FrameTransmitterConfig transmitterConfig;
    final FrameReceiverConfig receiverConfig;

    final FrameTransmitter transmitter;
    final FrameReceiver receiver;

    final long blockingTimeoutInSeconds;

    protected SoundCommunicator(@NonNull Context context, @NonNull String transmitterProfileKey, @NonNull String receiverProfileKey, long blockingTimeoutInSeconds) {
        try {
            transmitterConfig = new FrameTransmitterConfig(context, transmitterProfileKey);
            receiverConfig = new FrameReceiverConfig(context, receiverProfileKey);
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to create SoundCommunicator while building configs", ioException);
        }
        try {
            transmitter = new FrameTransmitter(transmitterConfig);
            receiver = new FrameReceiver(receiverConfig);
        } catch (ModemException modemException) {
            throw new RuntimeException("Failed to create SoundCommunicator while setting up transmitter and receiver", modemException);
        }
        this.blockingTimeoutInSeconds = blockingTimeoutInSeconds;

        // TODO: ask for permissions to record sound and check sound is on => TransmitterActivity
    }

    @SuppressLint("Assert")
    @Override
    public void blockingSend(@NonNull byte[] toSendMessage) throws IOException {
        if (toSendMessage.length > transmitter.getFrameLength()) {
            throw new IllegalArgumentException("To-send message is to long: " + toSendMessage.length + " > frame length " + transmitter.getFrameLength());
        }
        transmitter.setBlocking(blockingTimeoutInSeconds, 0); // maybe it's enough to be done only once in c-tor
        long sentBytesNumber = transmitter.send(toSendMessage);
        assert sentBytesNumber == toSendMessage.length;
    }

    @SuppressLint("Assert")
    @Override
    public void blockingReceive(@NonNull byte[] receivedMessageBuffer) throws IOException {
        if (receivedMessageBuffer.length < transmitter.getFrameLength()) {
            throw new IllegalArgumentException("Message buffer is to short: " + receivedMessageBuffer.length + " < frame length " + transmitter.getFrameLength());
        }
        long receivedBytesNumber = receiver.receive(receivedMessageBuffer);
        assert receivedBytesNumber <= transmitter.getFrameLength();
    }
}
