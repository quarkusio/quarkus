package io.quarkus.tls.runtime.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.runtime.util.ClassPathUtils;

public class TlsConfigUtils {

    private TlsConfigUtils() {
        // Avoid direct instantiation
    }

    /**
     * Read the content of the path.
     * <p>
     * The file is read from the classpath if it exists, otherwise it is read from the file system.
     *
     * @param path the path, must not be {@code null}
     * @return the content of the file
     */
    public static byte[] read(Path path) {
        byte[] data;
        try {
            final InputStream resource = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(ClassPathUtils.toResourceName(path));
            if (resource != null) {
                try (InputStream is = resource) {
                    data = is.readAllBytes();
                }
            } else {
                try (InputStream is = Files.newInputStream(path)) {
                    data = is.readAllBytes();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read file " + path, e);
        }
        return data;
    }

}
