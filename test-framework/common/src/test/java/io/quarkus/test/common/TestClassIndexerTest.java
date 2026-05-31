package io.quarkus.test.common;

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
     *
     * <p>
     * Multiple writer threads invoke {@link TestClassIndexer#writeIndex} against a single
     * target file, mirroring what happens when Gradle runs with {@code maxParallelForks > 1}
     * and several forks share the same {@code build/classes/java/test/}. The current
     * {@code writeIndex} opens {@code new FileOutputStream(file, false)} which truncates the
     * shared file on every call; concurrent writers therefore leave sparse-zero prefixes
     * (one writer's still-open file descriptor keeps writing at a non-zero offset after
     * another writer's {@code O_TRUNC} reset the inode).
     *
     * <p>
     * Reader threads exercise the production {@link TestClassIndexer#readIndex} path. When
     * a reader catches the file with a sparse-zero prefix, {@code IndexReader.readVersion}
     * throws {@code IllegalArgumentException("Not a jandex index")}; the current
     * {@code readIndex} only catches {@link java.io.IOException}, so the IAE escapes —
     * exactly the failure that kills Gradle test workers in the field.
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
}
