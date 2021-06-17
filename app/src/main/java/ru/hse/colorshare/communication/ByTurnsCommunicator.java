package ru.hse.colorshare.communication;

import android.app.Activity;

import androidx.annotation.NonNull;

import ru.hse.colorshare.communication.impl.SoniTalkByTurnCommunicator;

public interface ByTurnsCommunicator {
    /* fails if operations order is wrong:
     * only (send:receive)+ on one side and
     * firstTurnReceive:(send:receive):lastTurnSend on other side
     * are allowed */

    // use in onCreate only
    static ByTurnsCommunicator getInstance(Activity activity) {
        return new SoniTalkByTurnCommunicator(activity);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean checkPermissionsAreGranted(int requestCode, @NonNull int[] grantResults);
    // must be called at onRequestPermissionsResult

    void bindPartner(long partnerId);

    void send(Message message);
    // send message one time and submit it to further resending

    Message receive() throws ConnectionLostException; // returns null only if and only if was interrupted
    // receive next message with TIMEOUT_RECEIVE
    // => return message if it's new
    // => resend previously sent message if it's from previous turns
    // ConnectionLostException is thrown if max attempts number was exceed

    Message firstTurnReceive() throws ConnectionLostException;
    // receive any message with TIMEOUT_RECEIVE

    void lastTurnSend(Message message);
    // send message several times without waiting for response

    void shutdown();

}
