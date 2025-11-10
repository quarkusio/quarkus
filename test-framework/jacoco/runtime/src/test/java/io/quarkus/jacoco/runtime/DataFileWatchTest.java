package io.quarkus.jacoco.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DataFileWatchTest {

    // This is a rather long timeout due to the GH runner instances running many things concurrently.
    static final Duration FUTURE_WAIT_DURATION = Duration.ofMinutes(15);

    @TempDir
    Path tempDir;

    ExecutorService executor;

    @BeforeEach
    public void setup() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void teardown() throws InterruptedException {
        executor.shutdown();
        assertThat(executor.awaitTermination(1, TimeUnit.MINUTES)).isTrue();
    }

    @Test
    public void dataFileSize() throws Exception {
        Path datafile = tempDir.resolve("some-data-file.exec");

        DataFileWatch dataFileWatch = new DataFileWatch(datafile, () -> true, m -> {
        });

        assertThat(dataFileWatch.dataFileSize()).isEqualTo(-1L);

        Files.createFile(datafile);
        assertThat(dataFileWatch.dataFileSize()).isEqualTo(0L);

        Files.write(datafile, new byte[100]);
        assertThat(dataFileWatch.dataFileSize()).isEqualTo(100L);
    }

    @Test
    public void waitForDataFileGoodCase() {
        // Scenario:
        // - The Jacoco data file does not already exist
        // - File created after 2 wait iterations
        // - Jacoco shutdown hook finishes after 3 wait iterations.

        Path datafile = tempDir.resolve("some-data-file.exec");

        AtomicBoolean jacocoFinished = new AtomicBoolean(false);
        AtomicLong clock = new AtomicLong();
        AtomicInteger waitIterations = new AtomicInteger(5);
        AtomicReference<String> error = new AtomicReference<>();

        DataFileWatch dataFileWatch = new DataFileWatch(clock::get, datafile, jacocoFinished::get, error::set) {
            @Override
            void waitSleep() {
                switch (waitIterations.decrementAndGet()) {
                    case 3:
                        try {
                            Files.write(datafile, new byte[100]);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case 2:
                        jacocoFinished.set(true);
                        break;
                    case 1:
                    case 0:
                        fail();
                        break;
                }
            }
        };

        Future<Boolean> future = executor.submit(dataFileWatch::waitForDataFile);

        assertThat(future).succeedsWithin(FUTURE_WAIT_DURATION).isEqualTo(true);
        assertThat(waitIterations.get()).isEqualTo(2);
        assertThat(error.get()).isNull();
    }

    @Test
    public void waitForDataFileGoodCaseExists() throws Exception {
        // Scenario:
        // - The Jacoco data file already exists
        // - file updated after 2 wait iterations
        // - Jacoco shutdown hook finishes after 3 wait iterations.

        Path datafile = tempDir.resolve("some-data-file.exec");
        Files.write(datafile, new byte[100]);

        AtomicBoolean jacocoFinished = new AtomicBoolean(false);
        AtomicLong clock = new AtomicLong();
        AtomicInteger waitIterations = new AtomicInteger(5);
        AtomicReference<String> error = new AtomicReference<>();

        DataFileWatch dataFileWatch = new DataFileWatch(clock::get, datafile, jacocoFinished::get, error::set) {
            @Override
            void waitSleep() {
                switch (waitIterations.decrementAndGet()) {
                    case 3:
                        try {
                            Files.write(datafile, new byte[200]);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case 2:
                        jacocoFinished.set(true);
                        break;
                    case 1:
                    case 0:
                        fail();
                        break;
                }
            }
        };

        Future<Boolean> future = executor.submit(dataFileWatch::waitForDataFile);

        assertThat(future).succeedsWithin(FUTURE_WAIT_DURATION).isEqualTo(true);
        assertThat(waitIterations.get()).isEqualTo(2);
        assertThat(error.get()).isNull();
    }

    @Test
    public void waitForDataFileJacocoStillRunning() {
        // Scenario:
        // - The Jacoco data file does not already exist
        // - file created after 2 wait iterations
        // - Jacoco shutdown hook never finishes

        Path datafile = tempDir.resolve("some-data-file.exec");

        AtomicBoolean jacocoFinished = new AtomicBoolean(false);
        AtomicLong clock = new AtomicLong();
        AtomicInteger waitIterations = new AtomicInteger(5);
        AtomicReference<String> error = new AtomicReference<>();

        DataFileWatch dataFileWatch = new DataFileWatch(clock::get, datafile, jacocoFinished::get, error::set) {
            @Override
            void waitSleep() {
                if (waitIterations.get() > 0) {
                    if (waitIterations.decrementAndGet() == 2) {
                        try {
                            Files.write(datafile, new byte[100]);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    clock.addAndGet(500L);
                }
            }
        };

        Future<Boolean> future = executor.submit(dataFileWatch::waitForDataFile);

        assertThat(future).succeedsWithin(FUTURE_WAIT_DURATION).isEqualTo(false);
        assertThat(waitIterations.get()).isEqualTo(0);
        assertThat(error.get()).isEqualTo(String.format(
                "Timed out waiting for Jacoco data file %s update, file size before test run: did not exist, current file size: 100",
                datafile));
    }

    @Test
    public void waitForDataFileThatNeverAppears() {
        // Scenario:
        // - The Jacoco data file does not already exist,
        // - The data file never appears

        Path datafile = tempDir.resolve("some-data-file.exec");

        AtomicBoolean jacocoFinished = new AtomicBoolean(true);
        AtomicLong clock = new AtomicLong();
        AtomicInteger waitIterations = new AtomicInteger(5);
        AtomicReference<String> error = new AtomicReference<>();

        DataFileWatch dataFileWatch = new DataFileWatch(clock::get, datafile, jacocoFinished::get, error::set) {
            @Override
            void waitSleep() {
                if (waitIterations.decrementAndGet() == 0) {
                    clock.set(DataFileWatch.ABORT_TIMEOUT_MILLIS + 1);
                }
            }
        };

        Future<Boolean> future = executor.submit(dataFileWatch::waitForDataFile);

        assertThat(future).succeedsWithin(FUTURE_WAIT_DURATION).isEqualTo(false);
        assertThat(waitIterations.get()).isEqualTo(0);
        assertThat(error.get()).isEqualTo(String.format(
                "Timed out waiting for Jacoco data file %s update, file size before test run: did not exist, current file size: does not exist",
                datafile));
    }

    @Test
    public void waitForPreexistingDataFileThatNeverChanges() throws Exception {
        // Scenario:
        // - The Jacoco data file already exists
        // - The file never changes.

        Path datafile = tempDir.resolve("some-data-file.exec");

        Files.write(datafile, new byte[100]);

        AtomicBoolean jacocoFinished = new AtomicBoolean(true);
        AtomicInteger waitIterations = new AtomicInteger(5);
        AtomicLong clock = new AtomicLong();
        AtomicReference<String> error = new AtomicReference<>();

        DataFileWatch dataFileWatch = new DataFileWatch(clock::get, datafile, jacocoFinished::get, error::set) {
            @Override
            void waitSleep() {
                if (waitIterations.decrementAndGet() == 0) {
                    clock.set(DataFileWatch.ABORT_TIMEOUT_MILLIS + 1);
                }
            }
        };

        Future<Boolean> future = executor.submit(dataFileWatch::waitForDataFile);

        assertThat(future).succeedsWithin(FUTURE_WAIT_DURATION).isEqualTo(false);
        assertThat(waitIterations.get()).isEqualTo(0);
        assertThat(error.get()).isEqualTo(String.format(
                "Timed out waiting for Jacoco data file %s update, file size before test run: 100, current file size: 100",
                datafile));
    }
}
