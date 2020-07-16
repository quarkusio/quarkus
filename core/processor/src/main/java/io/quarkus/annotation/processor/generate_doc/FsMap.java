package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * A file system backed map.
 */
public class FsMap {

    private final Path dir;

    public FsMap(Path dir) {
        this.dir = safeCreateDirectories(dir);
    }

    public String get(String key) {
        final Path file = dir.resolve(key);
        if (Files.exists(file)) {
            try {
                return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + file, e);
            }
        } else {
            return null;
        }
    }

    public void put(String key, String value) {
        final Path file = dir.resolve(key);
        try {
            Files.write(file, value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write " + file, e);
        }
    }

    public boolean hasKey(String key) {
        final Path file = dir.resolve(key);
        return Files.exists(file);
    }

    /**
     * Attempts to call {@link Files#createDirectories(Path, java.nio.file.attribute.FileAttribute...)} with the given
     * {@code dir} at most {@code dir.getNameCount()} times as long as it does not exists, assuming that other treads
     * may try to create the same directory concurrently.
     *
     * @param dir the directory to create
     * @throws RuntimeException A wrapped {@link IOException} thrown by the last call to
     *         {@link Files#createDirectories(Path, java.nio.file.attribute.FileAttribute...)}
     */
    static Path safeCreateDirectories(Path dir) {
        IOException lastException;
        int retries = dir.getNameCount();
        do {
            if (Files.exists(dir)) {
                return dir;
            }
            try {
                Files.createDirectories(dir);
                return dir;
            } catch (IOException e) {
                lastException = e;
            }
        } while (retries-- > 0);
        throw new RuntimeException("Could not create directories " + dir, lastException);
    }

    public Properties asProperties() {
        final Properties result = new Properties();
        if (Files.exists(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                files
                        .filter(Files::isRegularFile)
                        .forEach(f -> {
                            try {
                                result.setProperty(f.getFileName().toString(),
                                        new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
                            } catch (IOException e) {
                                throw new IllegalStateException("Could not read from " + f, e);
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Could not list " + dir, e);
            }
        }
        return result;
    }

}
