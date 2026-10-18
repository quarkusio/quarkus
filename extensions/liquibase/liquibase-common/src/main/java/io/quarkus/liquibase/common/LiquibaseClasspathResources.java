package io.quarkus.liquibase.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import liquibase.changelog.DatabaseChangeLog;

public final class LiquibaseClasspathResources {

    private LiquibaseClasspathResources() {
    }

    /**
     * Reads a classpath resource using the same path normalization Liquibase uses for changelog paths.
     *
     * @return the resource bytes, or {@code null} if not found
     */
    public static byte[] readAllBytesOrNull(ClassLoader classLoader, String resourcePath) {
        String normalized = DatabaseChangeLog.normalizePath(resourcePath);
        byte[] fromNormalized = readStream(classLoader, normalized);
        if (fromNormalized != null) {
            return fromNormalized;
        }
        if (normalized.startsWith("/")) {
            return readStream(classLoader, normalized.substring(1));
        }
        return null;
    }

    private static byte[] readStream(ClassLoader classLoader, String path) {
        try (InputStream in = classLoader.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
