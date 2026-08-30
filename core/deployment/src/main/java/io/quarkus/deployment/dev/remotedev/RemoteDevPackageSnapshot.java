package io.quarkus.deployment.dev.remotedev;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class RemoteDevPackageSnapshot {

    private final Map<String, FileState> files;

    private RemoteDevPackageSnapshot(Map<String, FileState> files) {
        this.files = Map.copyOf(files);
    }

    public static RemoteDevPackageSnapshot empty() {
        return new RemoteDevPackageSnapshot(Map.of());
    }

    public static RemoteDevPackageSnapshot capture(Path packageRoot) throws IOException {
        Path normalizedRoot = packageRoot.toAbsolutePath().normalize();
        Map<String, FileState> files = new TreeMap<>();
        if (!Files.exists(normalizedRoot)) {
            return empty();
        }
        try (var paths = Files.walk(normalizedRoot)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                Path normalizedFile = file.toAbsolutePath().normalize();
                String relativePath = relativePath(normalizedRoot, normalizedFile);
                if (relativePath.equals("quarkus") || relativePath.startsWith("quarkus/")) {
                    continue;
                }
                files.put(relativePath, new FileState(sha1(normalizedFile), Files.size(normalizedFile)));
            }
        }
        return new RemoteDevPackageSnapshot(files);
    }

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

    public RemoteDevPackageDiff requestedFiles(Set<String> requestedPaths, Path packageRoot) {
        requireNonNull(requestedPaths, "requestedPaths");
        Path normalizedRoot = packageRoot.toAbsolutePath().normalize();
        List<RemoteDevPackageChange> changed = requestedPaths.stream()
                .map(RemoteDevPackageDeletePolicy::normalize)
                .sorted()
                .filter(files::containsKey)
                .map(path -> {
                    FileState state = files.get(path);
                    return new RemoteDevPackageChange(
                            path,
                            normalizedRoot.resolve(path).normalize(),
                            state.sha1(),
                            state.size());
                })
                .toList();
        return new RemoteDevPackageDiff(changed, List.of());
    }

    public Map<String, String> hashes() {
        Map<String, String> hashes = new TreeMap<>();
        files.forEach((path, state) -> hashes.put(path, state.sha1()));
        return hashes;
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    private static String relativePath(Path root, Path file) {
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Path " + file + " is not under package root " + root);
        }
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static String sha1(Path file) {
        MessageDigest digest = sha1();
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to hash remote-dev package file " + file, e);
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
