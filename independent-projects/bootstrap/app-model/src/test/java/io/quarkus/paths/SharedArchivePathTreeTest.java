package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.fs.util.ZipUtils;

public class SharedArchivePathTreeTest {
    private static final int WORKERS_COUNT = 128;
    private static final String BASE_DIR = "paths/directory-path-tree";

    private static Path testJar;

    @BeforeAll
    static void createTestJar() throws Exception {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(BASE_DIR + "/root");
        if (url == null) {
            throw new IllegalStateException("Failed to locate " + BASE_DIR + " on the classpath");
        }
        final Path rootDir = Path.of(url.toURI()).toAbsolutePath();
        testJar = rootDir.getParent().resolve("shared-root.jar");
        ZipUtils.zip(rootDir, testJar);
    }

    @AfterAll
    static void cleanup() {
        SharedArchivePathTree.removeFromCache(testJar);
    }

    @Test
    void walk() {
        final ArchivePathTree tree = SharedArchivePathTree.forPath(testJar);
        var visitor = new PathCollectingVisitor();
        tree.walk(visitor);
        assertThat(visitor.visitedPaths)
                .containsExactlyInAnyOrderEntriesOf(DirectoryPathTreeTest.getMultiReleaseMappedPaths());
    }

    @Test
    void accept() {
        final ArchivePathTree tree = SharedArchivePathTree.forPath(testJar);
        tree.accept("README.md", visit -> {
            assertThat(visit).isNotNull();
            assertThat(visit.getRelativePath("/")).isEqualTo("README.md");
            assertThat(visit.getRoot()).isEqualTo(testJar);
            try {
                assertThat(Files.readString(visit.getPath())).isEqualTo("test readme");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Test
    void apply() {
        final ArchivePathTree tree = SharedArchivePathTree.forPath(testJar);
        String content = tree.apply("README.md", visit -> {
            assertThat(visit).isNotNull();
            assertThat(visit.getRoot()).isEqualTo(testJar);
            try {
                return Files.readString(visit.getPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        assertThat(content).isEqualTo("test readme");
    }

    @Test
    void contains() {
        final ArchivePathTree tree = SharedArchivePathTree.forPath(testJar);
        assertThat(tree.contains("README.md")).isTrue();
        assertThat(tree.contains("non-existent")).isFalse();
    }

    @Test
    void nullPointerException() throws IOException, InterruptedException, ExecutionException {
        /* Reproduce https://github.com/quarkusio/quarkus/issues/48220 */
        stress((OpenPathTree opened) -> {
        });
    }

    @Test
    void closedFileSystemException() throws IOException, InterruptedException, ExecutionException {
        /* Reproduce https://github.com/quarkusio/quarkus/issues/48220 */
        stress((OpenPathTree opened) -> {
            try {
                Path p = opened.getPath("org/assertj/core/api/Assertions.class");
                Files.readAllBytes(p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void concurrentOpenSharesSingleFileSystem() throws Exception {
        /* Reproduce https://github.com/quarkusio/quarkus/issues/56987 */
        final Path jar = Path.of("target/shared-open-" + UUID.randomUUID() + ".jar");
        Files.createDirectories(jar.getParent());
        Files.copy(testJar, jar);
        final ArchivePathTree tree = SharedArchivePathTree.forPath(jar);

        final int openersCount = 16;
        final CyclicBarrier barrier = new CyclicBarrier(openersCount);
        final ExecutorService executor = Executors.newFixedThreadPool(openersCount);
        final List<OpenPathTree> openTrees = new ArrayList<>(openersCount);
        try {
            final List<Future<OpenPathTree>> futures = new ArrayList<>(openersCount);
            for (int i = 0; i < openersCount; i++) {
                futures.add(executor.submit(() -> {
                    barrier.await(30, TimeUnit.SECONDS);
                    return tree.open();
                }));
            }
            for (Future<OpenPathTree> future : futures) {
                openTrees.add(future.get(30, TimeUnit.SECONDS));
            }

            final FileSystem fs = getFileSystem(openTrees.get(0));
            for (OpenPathTree openTree : openTrees) {
                assertThat(getFileSystem(openTree)).isSameAs(fs);
            }

            // keep the first one open, the way a classloader would, and close the rest
            for (int i = 1; i < openTrees.size(); i++) {
                openTrees.get(i).close();
            }
            // a short-lived user should share the remaining open instance, closing it must not close the file system
            try (OpenPathTree shortLived = tree.open()) {
                assertThat(getFileSystem(shortLived)).isSameAs(fs);
            }
            assertThat(fs.isOpen()).isTrue();
            assertThat(Files.readString(openTrees.get(0).getPath("README.md"))).isEqualTo("test readme");
        } finally {
            executor.shutdown();
            for (OpenPathTree openTree : openTrees) {
                openTree.close();
            }
            SharedArchivePathTree.removeFromCache(jar);
        }
    }

    @Test
    void concurrentOpenAndManifestReadDoNotDeadlock() throws Exception {
        final int threadsCount = 8;
        final ExecutorService executor = Executors.newFixedThreadPool(threadsCount, r -> {
            // daemon threads, so that a deadlock does not prevent the JVM from exiting
            final Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        try {
            for (int i = 0; i < 100; i++) {
                // a new instance for each iteration, since the manifest is read only once per instance
                final ArchivePathTree tree = new SharedArchivePathTree(testJar);
                final CyclicBarrier barrier = new CyclicBarrier(threadsCount);
                final List<Future<Void>> futures = new ArrayList<>(threadsCount);
                for (int j = 0; j < threadsCount; j++) {
                    final boolean readManifest = j % 2 == 0;
                    futures.add(executor.submit(() -> {
                        barrier.await(30, TimeUnit.SECONDS);
                        if (readManifest) {
                            tree.getManifestAttributes();
                        } else {
                            try (OpenPathTree openTree = tree.open()) {
                                openTree.contains("README.md");
                            }
                        }
                        return null;
                    }));
                }
                for (Future<Void> future : futures) {
                    assertThat(future)
                            .describedAs("Expected no deadlock at iteration %d", i)
                            .succeedsWithin(30, TimeUnit.SECONDS);
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void closingWhileHoldingLockNeededByReaderDoesNotDeadlock() throws Exception {
        // a lock owned by the calling code, which is held while closing an open tree
        // and is also needed by a visitor walking the same open tree
        final ReentrantLock callerLock = new ReentrantLock();
        final ArchivePathTree tree = new SharedArchivePathTree(testJar);
        final int rounds = 20_000;
        // the last file systems used by the closer and the reader, they are expected to be closed in the end
        final AtomicReference<FileSystem> closerFs = new AtomicReference<>();
        final AtomicReference<FileSystem> readerFs = new AtomicReference<>();
        final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
            // daemon threads, so that a deadlock does not prevent the JVM from exiting
            final Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        try {
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final Future<Void> closer = executor.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                for (int i = 0; i < rounds; i++) {
                    final OpenPathTree openTree = tree.open();
                    closerFs.set(getFileSystem(openTree));
                    callerLock.lock();
                    try {
                        openTree.close();
                    } finally {
                        callerLock.unlock();
                    }
                }
                return null;
            });
            final Future<Void> reader = executor.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                for (int i = 0; i < rounds; i++) {
                    try (OpenPathTree openTree = tree.open()) {
                        readerFs.set(getFileSystem(openTree));
                        openTree.walk(visit -> {
                            callerLock.lock();
                            callerLock.unlock();
                        });
                    }
                }
                return null;
            });
            assertThat(closer).describedAs("Expected the closer not to deadlock").succeedsWithin(60, TimeUnit.SECONDS);
            assertThat(reader).describedAs("Expected the reader not to deadlock").succeedsWithin(60, TimeUnit.SECONDS);
            // closing must not have been skipped by the last user
            assertThat(closerFs.get().isOpen()).isFalse();
            assertThat(readerFs.get().isOpen()).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    private static FileSystem getFileSystem(OpenPathTree openTree) {
        return openTree.getPath("README.md").getFileSystem();
    }

    static void stress(Consumer<OpenPathTree> consumer) throws IOException {
        /* Find assertj-core jar in the class path */
        final String rawCp = System.getProperty("java.class.path");
        final String assertjCoreJarPath = Stream.of(rawCp.split(System.getProperty("path.separator")))
                .filter(p -> p.contains("assertj-core"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not find assertj-core  in " + rawCp));

        /* Create a copy of assertj-core jar in target directory */
        final Path assertjCoreJarPathCopy = Path.of("target/assertj-core-" + UUID.randomUUID() + ".jar");
        if (!Files.exists(assertjCoreJarPathCopy.getParent())) {
            Files.createDirectories(assertjCoreJarPathCopy.getParent());
        }
        Files.copy(Path.of(assertjCoreJarPath), assertjCoreJarPathCopy);

        /* Now do some concurrent opening and closing of the SharedArchivePathTree instance */
        final ArchivePathTree archivePathTree = SharedArchivePathTree.forPath(assertjCoreJarPathCopy);
        final ExecutorService executor = Executors.newFixedThreadPool(WORKERS_COUNT);
        final List<Future<Void>> futures = new ArrayList<>(WORKERS_COUNT);
        try {
            for (int i = 0; i < WORKERS_COUNT; i++) {
                final Future<Void> f = executor.submit(() -> {
                    try (OpenPathTree opened = archivePathTree.open()) {
                        consumer.accept(opened);
                    }
                    return null;
                });
                futures.add(f);
            }

            // Ensure all tasks are completed
            int i = 0;
            for (Future<Void> future : futures) {
                Assertions.assertThat(future)
                        .describedAs("Expected success at iteration %d", i++)
                        .succeedsWithin(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdown();
        }
    }

}
