package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestClassIndexerTest {

    /**
     * Reproduces https://github.com/quarkusio/quarkus/issues/54579.
     */
    @Test
    void writeIndex_concurrentWritersAndReaders_neverObserveCorruptIndex(@TempDir Path tmpDir) throws Exception {
        // A non-trivial index (the Jandex jar itself, always on the test classpath) keeps
        // the write window wide enough for polling readers to observe the race quickly.
        Path jandexJar = Path.of(Index.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Index index = TestClassIndexer.indexTestClasses(jandexJar);
        TestClassIndexer.writeIndex(index, tmpDir, getClass());

        int writers = 4;
        int readers = 2;
        int iterations = 200;
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean writersDone = new AtomicBoolean(false);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(writers + readers);
        try {
            List<Future<?>> writerFutures = new ArrayList<>();
            for (int w = 0; w < writers; w++) {
                writerFutures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < iterations && failure.get() == null; i++) {
                        TestClassIndexer.writeIndex(index, tmpDir, getClass());
                    }
                    return null;
                }));
            }
            for (int r = 0; r < readers; r++) {
                pool.submit(() -> {
                    start.await();
                    while (!writersDone.get() && failure.get() == null) {
                        try {
                            TestClassIndexer.readIndex(tmpDir, getClass());
                        } catch (RuntimeException e) {
                            failure.compareAndSet(null, e);
                        }
                    }
                    return null;
                });
            }
            start.countDown();
            for (Future<?> f : writerFutures) {
                f.get(60, TimeUnit.SECONDS);
            }
            writersDone.set(true);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }

        if (failure.get() != null) {
            throw new AssertionError("Concurrent writeIndex caused readIndex to fail", failure.get());
        }
    }

    /**
     * Defence-in-depth for the same race as
     * {@link #writeIndex_concurrentWritersAndReaders_neverObserveCorruptIndex}.
     */
    @Test
    void readIndex_recoversFromCorruptIndexFile(@TempDir Path tmpDir) throws IOException {
        Path indexFile = tmpDir.resolve(TestClassIndexer.TEST_CLASSES_IDX);
        // Arbitrary bytes that do not start with the jandex magic; this is exactly the
        // shape a torn write leaves behind (a sparse-zero or stale prefix).
        Files.write(indexFile, new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 });

        Index index = TestClassIndexer.readIndex(tmpDir, TestClassIndexerTest.class);

        assertThat(index).isNotNull();
        assertThat(index.getClassByName(TestClassIndexerTest.class.getName()))
                .as("fallback should re-index the test classes location")
                .isNotNull();
    }
}
