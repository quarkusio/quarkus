package io.quarkus.gradle.application.internal.dev;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.deployment.dev.BuildOutputChangeKind;

public final class GradleDevOutputSnapshot {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final HexFormat HEX = HexFormat.of();

    private final Map<FileKey, FileState> files;

    private GradleDevOutputSnapshot(Map<FileKey, FileState> files) {
        this.files = Map.copyOf(files);
    }

    public static GradleDevOutputSnapshot capture(List<Root> roots) throws IOException {
        var files = new LinkedHashMap<FileKey, FileState>();
        for (Root root : roots) {
            captureRoot(root, files);
        }
        return new GradleDevOutputSnapshot(files);
    }

    public static GradleDevOutputSnapshot captureEmpty() {
        return new GradleDevOutputSnapshot(Map.of());
    }

    public static GradleDevOutputSnapshot read(Path file) throws IOException {
        var files = new LinkedHashMap<FileKey, FileState>();
        if (!Files.exists(file)) {
            return new GradleDevOutputSnapshot(files);
        }
        for (String line : Files.readAllLines(file, UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length != 5) {
                throw new IOException("Invalid dev output snapshot record in " + file + ": " + line);
            }
            var key = new FileKey(
                    GradleDevOutputScope.valueOf(parts[0]),
                    decodePath(parts[1]),
                    decodePath(parts[2]));
            if (!isSha256(parts[4])) {
                return new GradleDevOutputSnapshot(Map.of());
            }
            files.put(key, new FileState(Long.parseLong(parts[3]), parts[4]));
        }
        return new GradleDevOutputSnapshot(files);
    }

    public void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        var lines = new ArrayList<String>(files.size());
        for (Map.Entry<FileKey, FileState> entry : files.entrySet()) {
            FileKey key = entry.getKey();
            FileState state = entry.getValue();
            lines.add(key.scope().name()
                    + "\t" + encodePath(key.outputRoot())
                    + "\t" + encodePath(key.changedPath())
                    + "\t" + state.size()
                    + "\t" + state.sha256());
        }
        Files.write(file, lines, UTF_8);
    }

    public List<GradleDevFileChange> changesSince(GradleDevOutputSnapshot previous) {
        var changes = new ArrayList<GradleDevFileChange>();
        for (Map.Entry<FileKey, FileState> entry : files.entrySet()) {
            FileState previousState = previous.files.get(entry.getKey());
            if (previousState == null) {
                changes.add(entry.getKey().toChange(BuildOutputChangeKind.ADDED));
            } else if (!previousState.equals(entry.getValue())) {
                changes.add(entry.getKey().toChange(BuildOutputChangeKind.MODIFIED));
            }
        }
        for (FileKey key : previous.files.keySet()) {
            if (!files.containsKey(key)) {
                changes.add(key.toChange(BuildOutputChangeKind.DELETED));
            }
        }
        return changes;
    }

    public int runtimeJarChangesSince(GradleDevOutputSnapshot previous) {
        int count = 0;
        for (GradleDevFileChange change : changesSince(previous)) {
            if (change.scope() == GradleDevOutputScope.RUNTIME_JARS) {
                count++;
            }
        }
        return count;
    }

    public GradleDevOutputSnapshot updatedBy(List<GradleDevFileChange> changes) throws IOException {
        var updated = new LinkedHashMap<>(files);
        for (GradleDevFileChange change : changes) {
            FileKey key = new FileKey(change.scope(), change.outputRoot().normalize(), change.changedPath().normalize());
            if (change.kind() == BuildOutputChangeKind.DELETED) {
                removeDeleted(updated, key);
            } else if (Files.isRegularFile(key.changedPath()) && shouldTrack(key.scope(), key.changedPath())) {
                captureFile(key.scope(), key.outputRoot(), key.changedPath(), updated);
            } else {
                updated.remove(key);
            }
        }
        return new GradleDevOutputSnapshot(updated);
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    private static void removeDeleted(Map<FileKey, FileState> files, FileKey deleted) {
        files.remove(deleted);
        files.keySet().removeIf(key -> key.scope() == deleted.scope()
                && key.outputRoot().equals(deleted.outputRoot())
                && key.changedPath().startsWith(deleted.changedPath()));
    }

    private static void captureRoot(Root root, Map<FileKey, FileState> files) throws IOException {
        Path rootPath = root.path().normalize();
        if (!Files.exists(rootPath)) {
            return;
        }
        if (Files.isRegularFile(rootPath)) {
            captureFile(root.scope(), rootPath, rootPath, files);
            return;
        }
        try (Stream<Path> paths = Files.walk(rootPath)) {
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                if (Files.isRegularFile(path) && shouldTrack(root.scope(), path)) {
                    captureFile(root.scope(), rootPath, path.normalize(), files);
                }
            }
        }
    }

    private static boolean shouldTrack(GradleDevOutputScope scope, Path path) {
        if (scope == GradleDevOutputScope.MAIN_CLASSES || scope == GradleDevOutputScope.DEPENDENCY_CLASSES) {
            return path.getFileName().toString().endsWith(".class");
        }
        return true;
    }

    private static void captureFile(GradleDevOutputScope scope, Path outputRoot, Path path, Map<FileKey, FileState> files)
            throws IOException {
        files.put(new FileKey(scope, outputRoot, path), new FileState(Files.size(path), sha256(path)));
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available", e);
        }
        try (InputStream input = Files.newInputStream(path);
                DigestInputStream digestInput = new DigestInputStream(input, digest)) {
            digestInput.transferTo(OutputStream.nullOutputStream());
        }
        return HEX.formatHex(digest.digest());
    }

    private static String encodePath(Path path) {
        return ENCODER.encodeToString(path.toString().getBytes(UTF_8));
    }

    private static Path decodePath(String value) {
        return Path.of(new String(DECODER.decode(value), UTF_8));
    }

    private static boolean isSha256(String value) {
        if (value.length() != 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    public record Root(GradleDevOutputScope scope, Path path) {
        public Root(GradleDevOutputScope scope, File file) {
            this(scope, file.toPath());
        }
    }

    private record FileKey(GradleDevOutputScope scope, Path outputRoot, Path changedPath) {
        GradleDevFileChange toChange(BuildOutputChangeKind kind) {
            return new GradleDevFileChange(scope, outputRoot, changedPath, kind);
        }
    }

    private record FileState(long size, String sha256) {
    }
}
