package io.quarkus.jacoco.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

class DataFileWatch {
    static final int ABORT_TIMEOUT_MILLIS = 10000;
    private final LongSupplier clock;
    private final Path datafile;
    private final long initialDataFileSize;
    private final BooleanSupplier jacocoFinished;
    private final Consumer<String> errorMsg;

    DataFileWatch(Path datafile, BooleanSupplier jacocoFinished, Consumer<String> errorMsg) {
        this(System::currentTimeMillis, datafile, jacocoFinished, errorMsg);
    }

    // Constructor for tests
    DataFileWatch(LongSupplier clock, Path datafile, BooleanSupplier jacocoFinished, Consumer<String> errorMsg) {
        this.clock = clock;
        this.datafile = datafile;
        this.jacocoFinished = jacocoFinished;
        this.errorMsg = errorMsg;

        // Remember the initial size of the data file. If the data file does not already exist,
        // the value will be -1, otherwise (if `JacocoConfig.reuseDataFile` is true and the file
        // exists) the "old" size of the data file.
        // In other words: Jacoco wrote the data file, if the file size is different from the
        // value of `initialDataFileSize`.
        try {
            this.initialDataFileSize = dataFileSize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    boolean waitForDataFile() throws IOException, InterruptedException {
        // The jacoco data is also generated by a shutdown hook.
        // Wait at most 10 seconds until the report data file appears, also wait as long as the report data
        // file grows, giving jacoco some "grace time" to write the next chunk of data, exit the loop when
        // either the "abort timeout" is hit or the jacoco thread is no longer running.
        long abortTime = nextAbortTime();
        long fileSize;
        for (long previousFileSize = initialDataFileSize;; previousFileSize = fileSize) {
            fileSize = dataFileSize();
            if (fileSize != initialDataFileSize) {
                if (fileSize != previousFileSize) {
                    // Give the jacoco thread writing the data file some time to write remaining data.
                    abortTime = nextAbortTime();
                }
                if (fileSize > 0L && jacocoFinished.getAsBoolean()) {
                    // Stop waiting when the Jacoco thread has stopped.
                    return true;
                }
            }
            if (timedOut(abortTime)) {
                errorMsg.accept("Timed out waiting for Jacoco data file " + datafile + " update, file size before test run: "
                        + (initialDataFileSize != -1L ? initialDataFileSize : "did not exist") + ", current file size: "
                        + (fileSize != -1L ? fileSize : "does not exist"));
                return false;
            }
            waitSleep();
        }
    }

    private boolean timedOut(long abortTime) {
        return abortTime - clock.getAsLong() < 0L;
    }

    private long nextAbortTime() {
        return clock.getAsLong() + ABORT_TIMEOUT_MILLIS;
    }

    // overridden in tests to let those complete quickly
    void waitSleep() throws InterruptedException {
        Thread.sleep(100L);
    }

    // visible for testing
    long dataFileSize() throws IOException {
        return Files.exists(datafile) ? Files.size(datafile) : -1L;
    }
}
