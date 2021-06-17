package ru.hse.colorshare.receiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.hse.colorshare.BuildConfig;
import ru.hse.colorshare.coding.decoding.DecodingController;

import static androidx.core.content.FileProvider.getUriForFile;

public class ReceiverController {

    private static final String TAG = "ReceiverController";

    private static final String tempFileName = "receiving.tmp";
    private static Uri contentTempFileUri;

    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    private static final long TIMEOUT_BULK_PROCESSING = 5000;

    private final ReceiverCameraActivity callerActivity;
    private final ExecutorService fileWriterExecutor;
    private final AtomicBoolean isWorking = new AtomicBoolean(false);
    private final AtomicBoolean isSuccess = new AtomicBoolean(false);

    public static DecodingController decodingController;
    private final String receivedFileName = "image-55555.jpg"; // ! hardcoded
    public static volatile int currentBulk = -1;
    private final Thread mainReceiverThread;

    public ReceiverController(ReceiverCameraActivity callerActivity, Thread.UncaughtExceptionHandler exceptionHandler) throws FileNotFoundException {
        Thread.UncaughtExceptionHandler receiverExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                if (fileWriterExecutor != null) {
                    fileWriterExecutor.shutdown();
                }
                exceptionHandler.uncaughtException(t, e);
            }
        };
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setUncaughtExceptionHandler(receiverExceptionHandler);
                return t;
            }
        };

        this.callerActivity = callerActivity;
        fileWriterExecutor = Executors.newSingleThreadExecutor(threadFactory);
        isWorking.set(true);

        File tempFile = new File(callerActivity.getFilesDir(), tempFileName);
        contentTempFileUri = FileProvider.getUriForFile(callerActivity.getApplicationContext(), BuildConfig.APPLICATION_ID + ".fileprovider", tempFile);
        Log.d(TAG, contentTempFileUri.toString());

        decodingController = DecodingController.create(contentTempFileUri, callerActivity.getApplicationContext());

        // trick to register callback before activity starts
        onFileCreateResultRunnable = this::onFileCreateResult;

        isWorking.set(true);
        mainReceiverThread = threadFactory.newThread(this::lifecycle);
    }

    public void start() {
        mainReceiverThread.start();
    }

    private void lifecycle() {
        Log.d(TAG, "lifecycle started");
        // receive starting msg
        // send smth back
        while (isWorking.get() && !Thread.interrupted()) {
            // receive next bulk msg
            // tell decoder about new bulk
            // update bulk number in ImageProcessor
            // await bulk result
            // flush decoder to file (executor)
            // break if read all bulks

            // ! hardcode:
            long[] checksums = new long[]{1555076429, 1154175259};
            decodingController.startNewBulkEncoding(checksums);
            currentBulk = 5;
            try {
                decodingController.awaitBulkFullyEncoded(TIMEOUT_BULK_PROCESSING, timeUnit);
            } catch (InterruptedException e) {
                break;
            }

            if (!decodingController.isBulkFullyEncoded()) {
                // timed out
                shutdown(new RuntimeException("bulk processing too long"));
            }
            fileWriterExecutor.submit(this::fileWritingJob);
            isWorking.set(false);
            isSuccess.set(true);

        }
        Log.d(TAG, "out of cycle");

        shutdown(null);
        if (isSuccess.get()) {
            // call saving activity
            saveResult();
        } else {
            // ! ???
        }
    }

    private void shutdown(RuntimeException err) {
        fileWriterExecutor.shutdown();
        isWorking.set(false);
        mainReceiverThread.interrupt();
        if (err != null) {
            throw err;
        }
    }

    public static volatile ActivityResultCallback<ActivityResult> onFileCreateCallback = new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            activityResult = result;
            onFileCreateResultRunnable.run();
        }
    };

    private void saveResult() {
        Log.d(TAG, "saving result");
        callerActivity.callFileCreate();
    }

    private static ActivityResult activityResult;
    private static Runnable onFileCreateResultRunnable;

    private void onFileCreateResult() {
        if (activityResult.getResultCode() == Activity.RESULT_OK) {
            // There are no request codes
            if (activityResult.getData() == null) {
                Log.w(TAG, "callback intent is null");
                saveFailed();
                return;
            }
            Uri targetFileUri = activityResult.getData().getData();
            if (targetFileUri == null) {
                Log.w(TAG, "null directory returned");
                saveFailed();
                return;
            }
            try {
                // Files.move and Paths.get is API 26 :(
                // directory pick returns a content uri anyway

                InputStream tempFileStream = callerActivity.getContentResolver().openInputStream(contentTempFileUri);
                OutputStream targetFileStream = callerActivity.getContentResolver().openOutputStream(targetFileUri);
                IOUtils.copy(tempFileStream, targetFileStream);
                tempFileStream.close();
                targetFileStream.close();

                Toast.makeText(callerActivity.getApplicationContext(), "File successfully saved!", Toast.LENGTH_SHORT).show();
                callerActivity.finish();

            } catch (IOException e) {
                Log.w(TAG, Log.getStackTraceString(e));
                saveFailed();
            }
        } else {
            Log.w(TAG, "directory pick unsuccessful with code " + activityResult.getResultCode());
            saveFailed();
        }
    }

    private void saveFailed() {
        Log.d(TAG, "saving failed");
        // ! doesn't show up !
        new AlertDialog.Builder(callerActivity)
                .setTitle("File saving failed")
                .setMessage("We have received a file successfully, but something went wrong and we couldn't save it. Try again?")
                .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        saveResult();
                    }
                })
                .setNegativeButton("Cancel, discard file", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        callerActivity.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void fileWritingJob() {
        try {
            decodingController.flush();
        } catch (IOException e) {
            isSuccess.set(false);
            isWorking.set(false);
            shutdown(new RuntimeException(e));
        }
    }

}
