package ru.hse.colorshare.communication;

import android.annotation.SuppressLint;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ru.hse.colorshare.coding.encoding.DataFrameBulk;
import ru.hse.colorshare.transmitter.TransmissionParams;

public class CommunicationProtocol {

    public static class HelloMessage {
        public final long uniqueTransmissionKey;
        public final long fileToSendSize;

        private HelloMessage(long uniqueTransmissionKey, long fileToSendSize) {
            this.uniqueTransmissionKey = uniqueTransmissionKey;
            this.fileToSendSize = fileToSendSize;
        }

        private HelloMessage(byte[] byteArray, int length) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray, 0, length);
            uniqueTransmissionKey = byteBuffer.getLong();
            fileToSendSize = byteBuffer.getLong();
        }

        public static HelloMessage create(long uniqueTransmissionKey, long fileToSendSize) {
            return new HelloMessage(uniqueTransmissionKey, fileToSendSize);
        }

        public static HelloMessage parseFromByteArray(byte[] byteArray, int length) {
            if (length != getSizeInBytes()) {
                return null;
            }
            return new HelloMessage(byteArray, length);
        }

        public byte[] toByteArray() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(getSizeInBytes());
            byteBuffer.putLong(uniqueTransmissionKey);
            byteBuffer.putLong(fileToSendSize);
            return byteBuffer.array();
        }

        public static int getSizeInBytes() {
            return 2 * Long.SIZE / 8;
        }
    }

    public static class TransmitterMessage {
        public final long uniqueTransmissionKey;
        public final TransmissionState transmissionState;
        public final int bulkIndex;
        public final int gridRows; // in units
        public final int gridCols; // int units
        public final long[] checksums;

        private TransmitterMessage(long uniqueTransmissionKey, TransmissionState transmissionState, int bulkIndex, int gridRows, int gridCols, long[] checksums) {
            this.uniqueTransmissionKey = uniqueTransmissionKey;
            this.transmissionState = transmissionState;
            this.bulkIndex = bulkIndex;
            this.gridRows = gridRows;
            this.gridCols = gridCols;
            this.checksums = checksums;
        }

        @SuppressLint("Assert")
        private TransmitterMessage(byte[] byteArray, int length) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray, 0, length);
            this.uniqueTransmissionKey = byteBuffer.getLong();
            this.transmissionState = TransmissionState.values()[byteBuffer.getInt()];
            this.bulkIndex = byteBuffer.getInt();
            this.gridRows = byteBuffer.getInt();
            this.gridCols = byteBuffer.getInt();
            List<Long> checksumsList = new ArrayList<>();
            while (byteBuffer.remaining() >= Long.SIZE / 8) {
                checksumsList.add(byteBuffer.getLong());
            }
            assert !byteBuffer.hasRemaining();
            checksums = new long[checksumsList.size()];
            for (int i = 0; i < checksumsList.size(); i++) {
                checksums[i] = checksumsList.get(i);
            }
        }

        public static TransmitterMessage createInProgressMessage(long uniqueTransmissionKey, int bulkIndex, DataFrameBulk bulk, TransmissionParams transmissionParams) {
            return new TransmitterMessage(uniqueTransmissionKey, TransmissionState.IN_PROGRESS, bulkIndex, transmissionParams.rows, transmissionParams.cols, bulk.getChecksums());
        }

        public static TransmitterMessage createSuccessfullyFinishedMessage(long uniqueTransmissionKey) {
            return new TransmitterMessage(uniqueTransmissionKey, TransmissionState.SUCCESSFULLY_FINISHED, -1, -1, -1, new long[0]);
        }

        public static TransmitterMessage createCanceledMessage(long uniqueTransmissionKey) {
            return new TransmitterMessage(uniqueTransmissionKey, TransmissionState.CANCELED, -1, -1, -1, new long[0]);
        }

        public static TransmitterMessage parseFromByteArray(long uniqueTransmissionKey, byte[] byteArray, int length) {
            if (!checkTransmitterMessageCanHaveSuchSizeInBytes(length)) {
                return null;
            }
            TransmitterMessage transmitterMessage = new TransmitterMessage(byteArray, length);
            return transmitterMessage.uniqueTransmissionKey == uniqueTransmissionKey ? transmitterMessage : null;
        }

        public byte[] toByteArray() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(getSizeInBytes());
            byteBuffer.putLong(uniqueTransmissionKey);
            byteBuffer.getInt(transmissionState.ordinal());
            byteBuffer.putInt(bulkIndex);
            byteBuffer.putInt(gridRows);
            byteBuffer.putInt(gridCols);
            for (long checksum : checksums) {
                byteBuffer.putLong(checksum);
            }
            return byteBuffer.array();
        }

        private static boolean checkTransmitterMessageCanHaveSuchSizeInBytes(int size) {
            return (size - (3 * Integer.SIZE / 8 + Long.SIZE / 8)) % Long.SIZE == 0;
        }

        public int getSizeInBytes() {
            return 3 * Integer.SIZE / 8 + (checksums.length + 1) * Long.SIZE / 8;
        }

        public enum TransmissionState {
            IN_PROGRESS,
            SUCCESSFULLY_FINISHED,
            CANCELED
        }
    }

    public static class ReceiverMessage {
        public final long uniqueTransmissionKey;
        public final int bulkIndex;
        // TODO: add enum for response type
        // for now, BulkResponseMessage = success

        private ReceiverMessage(long uniqueTransmissionKey, int bulkIndex) {
            this.uniqueTransmissionKey = uniqueTransmissionKey;
            this.bulkIndex = bulkIndex;
        }

        private ReceiverMessage(byte[] byteArray, int length) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray, 0, length);
            uniqueTransmissionKey = byteBuffer.getLong();
            bulkIndex = byteBuffer.getInt();
        }

        public static ReceiverMessage create(long uniqueTransmissionKey, int bulkIndex) {
            return new ReceiverMessage(uniqueTransmissionKey, bulkIndex);
        }

        public static ReceiverMessage parseFromByteArray(long uniqueTransmissionKey, byte[] byteArray, int length) {
            if (length != getSizeInBytes()) {
                return null;
            }
            ReceiverMessage receiverMessage = new ReceiverMessage(byteArray, length);
            return receiverMessage.uniqueTransmissionKey == uniqueTransmissionKey ? receiverMessage : null;
        }

        public byte[] toByteArray() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(getSizeInBytes());
            byteBuffer.putLong(uniqueTransmissionKey);
            byteBuffer.putInt(bulkIndex);
            return byteBuffer.array();
        }

        public static int getSizeInBytes() {
            return Long.SIZE / 8 + Integer.SIZE / 8;
        }
    }
}