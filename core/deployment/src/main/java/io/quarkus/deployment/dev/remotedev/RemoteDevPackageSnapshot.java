package io.quarkus.deployment.dev.remotedev;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Immutable metadata snapshot of the regular files in a local remote-development package.
 * <p>
 * A snapshot produced by {@link #capture(Path)} records normalized package-relative paths, SHA-1 hashes, and sizes. It
 * does not retain the package root or copy file contents, so operations that produce {@link RemoteDevPackageChange}
 * values also receive the current package root. Capture excludes the top-level {@code quarkus} entry and its
 * descendants and rejects symbolic links and entries that are neither directories nor regular files.
 * <p>
 * The persisted representation written by {@link #write(Path)} is build-session state. It may contain assumptions
 * specific to the producing Quarkus version and is not a portable publication format.
 *
 * <p>
 * <strong>API note:</strong>
 * This class is an integration contract for aligned Quarkus build-tool components, not a general directory
 * snapshot API.
 */
public final class RemoteDevPackageSnapshot {

    private final Map<String, FileState> files;

    private RemoteDevPackageSnapshot(Map<String, FileState> files) {
        this.files = Map.copyOf(files);
    }

    /**
     * Creates a snapshot with no files.
     *
     * @return the empty snapshot
     */
    public static RemoteDevPackageSnapshot empty() {
        return new RemoteDevPackageSnapshot(Map.of());
    }

    /**
     * Captures the current package tree.
     *
     * @param packageRoot non-{@code null} package root; a nonexistent root produces {@link #empty()}
     * @return an immutable snapshot of accepted regular files
     * @throws IOException if the root cannot be inspected, is not a directory, or the tree contains a symbolic
     *         link or another unsupported entry
     * @throws NullPointerException if {@code packageRoot} is {@code null}
     */
    public static RemoteDevPackageSnapshot capture(Path packageRoot) throws IOException {
        Path normalizedRoot = packageRoot.toAbsolutePath().normalize();
        Map<String, FileState> files = new TreeMap<>();
        if (!Files.exists(normalizedRoot, NOFOLLOW_LINKS)) {
            return empty();
        }
        BasicFileAttributes rootAttributes = Files.readAttributes(normalizedRoot, BasicFileAttributes.class, NOFOLLOW_LINKS);
        if (rootAttributes.isSymbolicLink() || !rootAttributes.isDirectory()) {
            throw new IOException("Remote-dev package root has an unsupported entry type: " + normalizedRoot);
        }
        try (var paths = Files.walk(normalizedRoot)) {
            for (Path file : paths.toList()) {
                Path normalizedFile = file.toAbsolutePath().normalize();
                String relativePath = relativePath(normalizedRoot, normalizedFile);
                if (relativePath.isEmpty()) {
                    continue;
                }
                if (relativePath.equals("quarkus") || relativePath.startsWith("quarkus/")) {
                    continue;
                }
                BasicFileAttributes attributes = Files.readAttributes(normalizedFile, BasicFileAttributes.class,
                        NOFOLLOW_LINKS);
                if (attributes.isDirectory()) {
                    continue;
                }
                if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
                    throw unsupportedEntry(normalizedRoot, normalizedFile);
                }
                files.put(relativePath, new FileState(sha1(normalizedFile), attributes.size()));
            }
        }
        return new RemoteDevPackageSnapshot(files);
    }

    /**
     * Reads snapshot state previously written by {@link #write(Path)}.
     * <p>
     * The file is trusted build-session state. This reader validates its field shape and basic hash/size invariants,
     * but does not apply remote-request path policy to persisted paths.
     *
     * @param file non-{@code null} snapshot-state file; a nonexistent file produces {@link #empty()}
     * @return the reconstructed immutable snapshot
     * @throws IOException if the file cannot be read or a line does not have the expected field structure
     * @throws IllegalArgumentException if stored hash or size metadata is invalid
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static RemoteDevPackageSnapshot read(Path file) throws IOException {
        if (!Files.exists(file)) {
            return empty();
        }
        Map<String, FileState> files = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length != 3) {
                throw new IOException("Malformed remote-dev package snapshot line in " + file + ": " + line);
            }
            files.put(parts[0], new FileState(parts[1], Long.parseLong(parts[2])));
        }
        return new RemoteDevPackageSnapshot(files);
    }

    /**
     * Writes this snapshot as deterministic, tab-separated build-session state, creating parent directories as needed.
     * An existing file is replaced.
     *
     * @param file non-{@code null} destination state file
     * @throws IOException if the parent directory or file cannot be written
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public void write(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        StringBuilder content = new StringBuilder();
        files.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> content.append(entry.getKey())
                        .append('\t')
                        .append(entry.getValue().sha1())
                        .append('\t')
                        .append(entry.getValue().size())
                        .append('\n'));
        Files.writeString(file, content.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Computes the changes from {@code previous} to this snapshot.
     * <p>
     * This method does not read the package tree again. Changed entries point into the supplied current package root,
     * and the built-in HTTP client verifies their captured size and hash when sending them. Deletion candidates remain
     * subject to {@link RemoteDevPackageDeletePolicy}.
     *
     * @param previous non-{@code null} previously delivered snapshot
     * @param packageRoot non-{@code null} current package root corresponding to this snapshot
     * @return an immutable, deterministically ordered package difference
     * @throws NullPointerException if either argument is {@code null}
     */
    public RemoteDevPackageDiff diffSince(RemoteDevPackageSnapshot previous, Path packageRoot) {
        requireNonNull(previous, "previous");
        Path normalizedRoot = packageRoot.toAbsolutePath().normalize();
        List<RemoteDevPackageChange> changed = files.entrySet().stream()
                .filter(entry -> !entry.getValue().equals(previous.files.get(entry.getKey())))
                .map(entry -> new RemoteDevPackageChange(
                        entry.getKey(),
                        normalizedRoot.resolve(entry.getKey()).normalize(),
                        entry.getValue().sha1(),
                        entry.getValue().size()))
                .toList();
        List<String> deleted = previous.files.keySet().stream()
                .filter(path -> !files.containsKey(path))
                .toList();
        return new RemoteDevPackageDiff(changed, deleted);
    }

    /**
     * Creates a changed-file batch for the paths requested by the remote endpoint.
     * <p>
     * Requested paths are normalized to forward slashes and must be safe package-relative paths present in this
     * snapshot. Paths that collide after normalization are rejected.
     *
     * @param requestedPaths non-{@code null} paths requested by the endpoint
     * @param packageRoot non-{@code null} current package root corresponding to this snapshot
     * @return a difference containing only changed entries and no deletions
     * @throws IOException if a requested path is {@code null}, empty, unsafe, duplicated after normalization, or
     *         absent from this snapshot
     * @throws NullPointerException if {@code requestedPaths} or {@code packageRoot} is {@code null}
     */
    public RemoteDevPackageDiff requestedFiles(Set<String> requestedPaths, Path packageRoot) throws IOException {
        requireNonNull(requestedPaths, "requestedPaths");
        Path normalizedRoot = packageRoot.toAbsolutePath().normalize();
        Map<String, FileState> requested = new TreeMap<>();
        for (String requestedPath : requestedPaths) {
            if (requestedPath == null || requestedPath.isBlank()) {
                throw new IOException("Remote dev requested an empty package path");
            }
            String normalizedPath = RemoteDevPackageDeletePolicy.normalize(requestedPath);
            if (!RemoteDevPackageDeletePolicy.isSafeRelativePath(normalizedPath)) {
                throw new IOException("Remote dev requested an unsafe package path");
            }
            FileState state = files.get(normalizedPath);
            if (state == null) {
                throw new IOException("Remote dev requested package path that is absent from the current snapshot: "
                        + normalizedPath);
            }
            if (requested.put(normalizedPath, state) != null) {
                throw new IOException("Remote dev requested duplicate normalized package path: " + normalizedPath);
            }
        }
        List<RemoteDevPackageChange> changed = requested.entrySet().stream()
                .map(entry -> requestedChange(normalizedRoot, entry.getKey(), entry.getValue()))
                .toList();
        return new RemoteDevPackageDiff(changed, List.of());
    }

    /**
     * Returns a detached path-to-SHA-1 map suitable for the initial connection request.
     *
     * @return a new map in deterministic path order; mutating it does not affect this snapshot
     */
    public Map<String, String> hashes() {
        Map<String, String> hashes = new TreeMap<>();
        files.forEach((path, state) -> hashes.put(path, state.sha1()));
        return hashes;
    }

    /**
     * Returns whether this snapshot contains no accepted package files.
     *
     * @return {@code true} when the snapshot is empty
     */
    public boolean isEmpty() {
        return files.isEmpty();
    }

    private static String relativePath(Path root, Path file) {
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Path " + file + " is not under package root " + root);
        }
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static RemoteDevPackageChange requestedChange(Path normalizedRoot, String path, FileState state) {
        Path resolved = normalizedRoot.resolve(path).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Requested remote-dev package path escaped the package root");
        }
        return new RemoteDevPackageChange(path, resolved, state.sha1(), state.size());
    }

    private static IOException unsupportedEntry(Path root, Path entry) {
        return new IOException("Remote-dev package contains an unsupported entry type: "
                + relativePath(root, entry));
    }

    private static String sha1(Path file) throws IOException {
        MessageDigest digest = sha1();
        try (var input = Files.newInputStream(file, NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return hex(digest.digest());
    }

    private static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xf, 16));
            builder.append(Character.forDigit(value & 0xf, 16));
        }
        return builder.toString();
    }

    record FileState(String sha1, long size) {
        FileState {
            if (sha1 == null || sha1.isBlank()) {
                throw new IllegalArgumentException("Remote-dev package file hash must not be empty");
            }
            if (size < 0) {
                throw new IllegalArgumentException("Remote-dev package file size must not be negative");
            }
        }
    }
}
