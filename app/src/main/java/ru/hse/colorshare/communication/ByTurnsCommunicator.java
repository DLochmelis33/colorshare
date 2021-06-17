package ru.hse.colorshare.communication;

public interface ByTurnsCommunicator {
    // fails if operations order is wrong

    void send(Message message);
    // send message one time and submit it to further resending

    Message receive() throws ConnectionLostException;
    // receive next message
    // => return message if it's new
    // => resend previously sent message if it's from previous turns
    // ConnectionLostException is thrown if MAX_ATTEMPTS_NUMBER is surpassed

    // special THANKS DLochmelis33 !!!!

    Message firstTurnReceive() throws ConnectionLostException;

    void lastTurnSend();

}
