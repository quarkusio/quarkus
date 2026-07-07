package io.quarkus.gradle.application.internal.dev;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Owns the create-only initializer and atomic generation updates used to wake
 * a Gradle continuous build without exposing mutable session state through the
 * Gradle model.
 */
public final class ContinuousBuildTriggerFile implements AutoCloseable {

    private static final String INITIAL_VALUE = "epoch=initializer\ngeneration=0\n";

    private final Path path;
    private final String epoch;
    private long generation;
    private boolean closed;

    public ContinuousBuildTriggerFile(Path path, String epoch) {
        this(path, epoch, 0);
    }

    ContinuousBuildTriggerFile(Path path, String epoch, long generation) {
        this.path = requireNonNull(path, "path").toAbsolutePath().normalize();
        this.epoch = UUID.fromString(requireNonNull(epoch, "epoch")).toString();
        if (generation < 0) {
            throw new IllegalArgumentException("Continuous-build trigger generation must not be negative");
        }
        this.generation = generation;
    }

    public static void initialize(Path path) throws IOException {
        Path normalized = requireNonNull(path, "path").toAbsolutePath().normalize();
        Files.createDirectories(normalized.getParent());
        try {
            Files.writeString(normalized, INITIAL_VALUE, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
            // A live deployment owns every value after initialization.
        }
    }

    public synchronized boolean publishNext() throws IOException {
        if (closed) {
            return false;
        }
        return publish(Math.incrementExact(generation));
    }

    public synchronized boolean publish(long requestedGeneration) throws IOException {
        if (requestedGeneration < 0) {
            throw new IllegalArgumentException("Continuous-build trigger generation must not be negative");
        }
        if (closed || requestedGeneration <= generation) {
            return false;
        }
        write(requestedGeneration);
        generation = requestedGeneration;
        return true;
    }

    public Path path() {
        return path;
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    private void write(long requestedGeneration) throws IOException {
        Path parent = path.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, path.getFileName().toString() + ".", ".tmp");
        boolean moved = false;
        try {
            Files.writeString(temporary, "epoch=" + epoch + "\ngeneration=" + requestedGeneration + "\n",
                    StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
