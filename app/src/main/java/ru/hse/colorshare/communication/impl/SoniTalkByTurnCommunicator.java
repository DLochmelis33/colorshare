package ru.hse.colorshare.communication.impl;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import at.ac.fhstp.sonitalk.SoniTalkConfig;
import at.ac.fhstp.sonitalk.SoniTalkContext;
import at.ac.fhstp.sonitalk.SoniTalkDecoder;
import at.ac.fhstp.sonitalk.SoniTalkEncoder;
import at.ac.fhstp.sonitalk.SoniTalkMessage;
import at.ac.fhstp.sonitalk.SoniTalkPermissionsResultReceiver;
import at.ac.fhstp.sonitalk.SoniTalkSender;
import at.ac.fhstp.sonitalk.exceptions.ConfigException;
import at.ac.fhstp.sonitalk.exceptions.DecoderStateException;
import at.ac.fhstp.sonitalk.utils.ConfigFactory;
import at.ac.fhstp.sonitalk.utils.EncoderUtils;
import ru.hse.colorshare.communication.ByTurnsCommunicator;
import ru.hse.colorshare.communication.ConnectionLostException;
import ru.hse.colorshare.communication.Message;


public class SoniTalkByTurnCommunicator implements ByTurnsCommunicator, SoniTalkDecoder.MessageListener, SoniTalkPermissionsResultReceiver.Receiver {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 65;
    private final long uniqueId;
    private long partnerId;
    private Activity activity;

    private final static int MAX_RESENDING_ATTEMPTS = 6;
    private final static int RECEIVE_TIMEOUT_MILLIS = 10_000;

    private final static int FIRST_TURN_RECEIVE_TIMEOUT_MILLIS = 60_000;

    private final static int MAX_LAST_TURN_SEND_ATTEMPTS = 5;
    private final static int LAST_TURN_SEND_ATTEMPT_TIMEOUT_MILLIS = 1000;

    private final SoniTalkPermissionsResultReceiver soniTalkPermissionsResultReceiver;
    private final SoniTalkContext soniTalkContext;
    private SoniTalkDecoder soniTalkDecoder;
    private SoniTalkSender soniTalkSender;

    private AudioTrack playerFrequency = null;
    private final static int volume = 70; // from 1 to 100

    private final static int frequencyOffsetForSpectrogram = 50;
    private final static int stepFactor = 8;
    private final static int samplingRate = 44100;
    private final static int fftResolution = 4410;

    private final static int bitperiod = 100; // 30-200
    private final static int pauseperiod = 0; // 0-200
    private final static int f0 = 18000; // 50-20000
    private final static int nFrequencies = 16; // may be 18; 2-30 ?
    private final static int frequencySpace = 100; // 50-200
    private final static int nMaxBytes = 18; // 2-30 ?
    private final static int nMessageBlocks = (nMaxBytes + 2) / 2; // Default is 10 (transmitting 20 bytes with 16 frequencies)
    private final static int ignored = 0;

    private SoniTalkMessage lastSentSoniTalkMessage;
    private int messageId = 0;

    private final Lock messageReceivedLock = new ReentrantLock();
    private final Condition newMessageReceivedCondition = messageReceivedLock.newCondition();
    private Message receivedMessage;

    public SoniTalkByTurnCommunicator(Activity activity) { // must be used in activity onCreate only
        uniqueId = new Random().nextLong();
        this.activity = activity;
        soniTalkPermissionsResultReceiver = new SoniTalkPermissionsResultReceiver(new Handler());
        soniTalkPermissionsResultReceiver.setReceiver(this);
        soniTalkContext = SoniTalkContext.getInstance(activity, soniTalkPermissionsResultReceiver);

        if (!checkHasPermissions(activity)) {
            requestPermissions(activity);
        } else {
            startDecoder();
        }

    }

    private boolean checkHasPermissions(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions(Activity activity) {
        String[] permissions = {Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    @Override
    public boolean checkPermissionsAreGranted(int requestCode, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startDecoder();
            return true;
        }
        return false;
    }

    @Override
    public void bindPartner(long partnerId) {
        this.partnerId = partnerId;
    }

    @Override
    public void send(Message message) {
        message.setIds(messageId++, uniqueId);
        lastSentSoniTalkMessage = generateSoniTalkMessage(message);
        sendMessage(lastSentSoniTalkMessage);
    }

    @Override
    public Message receive() throws ConnectionLostException {
        messageReceivedLock.lock();
        try {
            for (int i = 0; i < MAX_RESENDING_ATTEMPTS; i++) {
                while (receivedMessage == null) {
                    try {
                        boolean timeoutIsNotElapsed = newMessageReceivedCondition.await(RECEIVE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                        if (!timeoutIsNotElapsed) {
                            throw new ConnectionLostException("RECEIVE_TIMEOUT_MILLIS elapsed");
                        }
                    } catch (InterruptedException interruptedException) {
                        return null;
                    }
                }
                int receivedMessageId = receivedMessage.getId();
                if (receivedMessageId == messageId) {
                    messageId++;
                    Message message = receivedMessage;
                    receivedMessage = null;
                    return message;
                }
                if (receivedMessageId > messageId) {
                    throw new IllegalStateException("received message id =" + receivedMessageId + " is greater than current expected = " + messageId);
                }
                if (receivedMessageId != messageId - 1) {
                    throw new IllegalStateException("received message id =" + receivedMessageId + " is less than current expected = " + messageId);
                }
                receivedMessage = null;
                sendMessage(lastSentSoniTalkMessage);
            }
        } finally {
            messageReceivedLock.unlock();
        }
        throw new ConnectionLostException("MAX_RESENDING_ATTEMPTS is exceeded");
    }

    @Override
    public Message firstTurnReceive() throws ConnectionLostException {
        messageReceivedLock.lock();
        try {
            while (receivedMessage == null) {
                try {
                    boolean timeoutIsNotElapsed = newMessageReceivedCondition.await(FIRST_TURN_RECEIVE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    if (!timeoutIsNotElapsed) {
                        throw new ConnectionLostException("FIRST_TURN_RECEIVE_TIMEOUT_MILLIS elapsed");
                    }
                } catch (InterruptedException interruptedException) {
                    return null;
                }
            }
            messageId++;
            Message message = receivedMessage;
            receivedMessage = null;
            return message;
        } finally {
            messageReceivedLock.unlock();
        }
    }

    @Override
    public void lastTurnSend(Message message) {
        message.setIds(messageId++, uniqueId);
        SoniTalkMessage soniTalkMessage = generateSoniTalkMessage(message);
        for (int i = 0; i < MAX_LAST_TURN_SEND_ATTEMPTS; i++) {
            sendMessage(soniTalkMessage);
            try {
                TimeUnit.MILLISECONDS.sleep(LAST_TURN_SEND_ATTEMPT_TIMEOUT_MILLIS);
            } catch (InterruptedException interruptedException) {
                return;
            }
        }
    }

    @Override
    public void shutdown() {
        activity = null;
        if (soniTalkSender != null) {
            soniTalkSender.cancel();
        }
        soniTalkSender = null;
        stopDecoder();
        soniTalkPermissionsResultReceiver.setReceiver(null);
    }

    private void startDecoder() {
        try {
            SoniTalkConfig config = ConfigFactory.getDefaultConfig(activity);
            config.setFrequencyZero(f0);
            config.setBitperiod(bitperiod);
            config.setPauseperiod(pauseperiod);
            config.setnMessageBlocks(nMessageBlocks);
            config.setnFrequencies(nFrequencies);
            config.setFrequencySpace(frequencySpace);

            assert soniTalkContext != null;
            soniTalkDecoder = soniTalkContext.getDecoder(samplingRate, config); //, stepFactor, frequencyOffsetForSpectrogram, silentMode);
            soniTalkDecoder.addMessageListener(this);
            soniTalkDecoder.receiveBackground(ignored);

        } catch (DecoderStateException | ConfigException | IOException exception) {
            shutdown();
            throw new RuntimeException(exception);
        }
    }

    private void stopDecoder() {
        if (soniTalkDecoder != null) {
            soniTalkDecoder.stopReceiving();
        }
        soniTalkDecoder = null;
    }

    private void sendMessage(SoniTalkMessage soniTalkMessage) {
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(3, (int) Math.round((audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * volume / 100.0D)), 0);

        assert soniTalkContext != null;
        soniTalkSender = soniTalkContext.getSender();
        soniTalkSender.send(soniTalkMessage, ignored);
    }

    private SoniTalkMessage generateSoniTalkMessage(Message message) {
        if (playerFrequency != null) {
            playerFrequency.stop();
            playerFrequency.flush();
            playerFrequency.release();
            playerFrequency = null;
        }

        SoniTalkConfig config = new SoniTalkConfig(f0, bitperiod, pauseperiod, nMessageBlocks, nFrequencies, frequencySpace);
        assert soniTalkContext != null;
        SoniTalkEncoder soniTalkEncoder = soniTalkContext.getEncoder(config);

        byte[] messageBytes = Message.toByteArray(message);
        if (messageBytes.length > nMaxBytes || EncoderUtils.isAllowedByteArraySize(messageBytes, config)) {
            throw new IllegalArgumentException("message is too long, nMaxBytes = " + nMaxBytes);
        }
        return soniTalkEncoder.generateMessage(messageBytes);
    }

    @Override
    public void onMessageReceived(SoniTalkMessage receivedSoniTalkMessage) {
        if (!receivedSoniTalkMessage.isCrcCorrect()) {
            return;
        }
        messageReceivedLock.lock();
        try {
            assert this.receivedMessage == null;
            Message receivedMessage = Message.parseFrom(receivedSoniTalkMessage.getMessage());
            if (receivedMessage.getSenderId() != partnerId) {
                return;
            }
            this.receivedMessage = receivedMessage;
            newMessageReceivedCondition.signal();
        } finally {
            messageReceivedLock.unlock();
        }
    }

    @Override
    public void onDecoderError(String errorMessage) {
        // docs: for example, microphone is broken
        shutdown();
        throw new RuntimeException("Decoder error");
    }

    @Override
    public void onSoniTalkPermissionResult(int resultCode, Bundle resultData) {
    }
}
