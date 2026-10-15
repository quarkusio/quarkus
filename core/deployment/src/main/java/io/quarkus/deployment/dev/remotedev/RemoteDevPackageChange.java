package io.quarkus.deployment.dev.remotedev;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

public record RemoteDevPackageChange(
        String relativePath,
        Path file,
        String sha1,
        long size) {

    public RemoteDevPackageChange {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Remote-dev package change path must not be empty");
        }
        requireNonNull(file, "file");
        if (sha1 == null || sha1.isBlank()) {
            throw new IllegalArgumentException("Remote-dev package change hash must not be empty");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Remote-dev package change size must not be negative");
        }
    }
}
