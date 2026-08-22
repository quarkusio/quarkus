package io.quarkus.deployment.dev.remotedev;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

/**
 * A changed regular file captured from a local remote-development package.
 * <p>
 * The file is not copied into this value. A client reads it during delivery and may verify that its size and digest
 * still match the captured metadata.
 * <p>
 * <strong>API note:</strong>
 * This value is an internal Quarkus build-tool integration contract and is meaningful only for the package snapshot
 * and client from the same session.
 *
 * @param relativePath non-blank path identifying the file relative to the package root; producers normally use
 *        forward slashes, but this record does not normalize or otherwise validate the path
 * @param file non-{@code null} local path from which the client will read the content
 * @param sha1 non-blank SHA-1 digest captured for the file; the constructor does not validate its representation
 * @param size non-negative size captured for the file
 */
public record RemoteDevPackageChange(
        String relativePath,
        Path file,
        String sha1,
        long size) {

    /**
     * Validates the required captured metadata.
     *
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IllegalArgumentException if {@code relativePath} or {@code sha1} is {@code null} or blank, or
     *         {@code size} is negative
     */
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
