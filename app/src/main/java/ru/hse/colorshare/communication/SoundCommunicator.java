package ru.hse.colorshare.communication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

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

    private static final String LOG_TAG = "ColorShare:communicator";

    protected SoundCommunicator(@NonNull Context context, @NonNull String transmitterProfileKey, @NonNull String receiverProfileKey) {
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
        Log.d(LOG_TAG, "SoundCommunicator constructed");
        // TODO: ask for permissions to record sound and check sound is on => TransmitterActivity
    }

    @SuppressLint("Assert")
    @Override
    public void blockingSend(@NonNull byte[] toSendMessage, long blockingTimeoutInSeconds) throws IOException {
        assert toSendMessage.length <= transmitter.getFrameLength();
        transmitter.setBlocking(blockingTimeoutInSeconds, 0);
        long sentBytesNumber = transmitter.send(toSendMessage);
        assert sentBytesNumber == toSendMessage.length;
        Log.d(LOG_TAG, sentBytesNumber + " message bytes were successfully sent");
    }

    @SuppressLint("Assert")
    @Override
    public long blockingReceive(@NonNull byte[] receivedMessageBuffer, long blockingTimeoutInSeconds) throws IOException {
        assert receivedMessageBuffer.length >= transmitter.getFrameLength();
        receiver.setBlocking(blockingTimeoutInSeconds, 0);
        long receivedBytesNumber = receiver.receive(receivedMessageBuffer);
        Log.d(LOG_TAG, receivedBytesNumber + " message bytes were successfully received");
        return receivedBytesNumber;
    }

    @Override
    public void close() {
        if (transmitter != null) {
            transmitter.close();
        }
        if (receiver != null) {
            receiver.close();
        }
        Log.d(LOG_TAG, "SoundCommunicator was successfully closed");
    }
}
