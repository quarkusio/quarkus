package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
    void recoveryAfterBrokenFileSystem() {
        final ArchivePathTree tree = SharedArchivePathTree.forPath(testJar);

        // Simulate a mid-read interrupt that closes the shared FileChannel (JDK-8316882)
        assertThatThrownBy(() -> tree.accept("README.md", visit -> {
            throw new UncheckedIOException(new ClosedChannelException());
        })).isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(ClosedChannelException.class);

        // The next call should recover by opening a fresh filesystem
        tree.accept("README.md", visit -> {
            assertThat(visit).isNotNull();
            try {
                assertThat(Files.readString(visit.getPath())).isEqualTo("test readme");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
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
