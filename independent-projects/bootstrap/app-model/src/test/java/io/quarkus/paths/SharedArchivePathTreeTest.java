package io.quarkus.paths;

import java.io.IOException;
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
import org.junit.jupiter.api.Test;

public class SharedArchivePathTreeTest {
    private static final int WORKERS_COUNT = 128;

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
