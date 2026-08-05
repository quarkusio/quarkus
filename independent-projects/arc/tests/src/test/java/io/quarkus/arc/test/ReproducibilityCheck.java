package io.quarkus.arc.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.jar.Manifest;

import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

class ReproducibilityCheck {

    private static final Map<String, Object> VINEFLOWER_OPTIONS;

    static {
        Map<String, Object> options = new HashMap<>(IFernflowerPreferences.getDefaults());
        options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "0");
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
        VINEFLOWER_OPTIONS = Map.copyOf(options);
    }

    record Diff(TreeSet<String> missing, TreeSet<String> extra, TreeSet<String> changed) {

        boolean isEmpty() {
            return missing.isEmpty() && extra.isEmpty() && changed.isEmpty();
        }

        @Override
        public String toString() {
            return "missing=" + missing.size() + " [" + String.join(", ", missing) + "]"
                    + ", extra=" + extra.size() + " [" + String.join(", ", extra) + "]"
                    + ", changed=" + changed.size() + " [" + String.join(", ", changed) + "]";
        }
    }

    static Diff diff(Map<String, byte[]> reference, Map<String, byte[]> current) {
        TreeSet<String> missing = new TreeSet<>(reference.keySet());
        missing.removeAll(current.keySet());

        TreeSet<String> extra = new TreeSet<>(current.keySet());
        extra.removeAll(reference.keySet());

        TreeSet<String> changed = new TreeSet<>();
        for (Map.Entry<String, byte[]> entry : reference.entrySet()) {
            byte[] currentBytes = current.get(entry.getKey());
            if (currentBytes != null && !Arrays.equals(entry.getValue(), currentBytes)) {
                changed.add(entry.getKey());
            }
        }
        return new Diff(missing, extra, changed);
    }

    static Path dumpMismatch(Diff diff, Map<String, byte[]> reference, Map<String, byte[]> current,
            int run, Path baseDir) {
        Path run1Dir = baseDir.resolve("run-1");
        Path runNDir = baseDir.resolve("run-" + run);
        StringBuilder index = new StringBuilder();
        index.append("run-1 vs run-").append(run).append('\n');

        try {
            Files.createDirectories(run1Dir);
            Files.createDirectories(runNDir);

            for (String resource : diff.missing()) {
                writeClass(run1Dir, resource, reference.get(resource));
                index.append("MISSING_IN_RUN_").append(run).append(' ').append(resource)
                        .append(" run-1-sha256=").append(sha256(reference.get(resource))).append('\n');
            }
            for (String resource : diff.extra()) {
                writeClass(runNDir, resource, current.get(resource));
                index.append("EXTRA_IN_RUN_").append(run).append(' ').append(resource)
                        .append(" run-").append(run).append("-sha256=").append(sha256(current.get(resource))).append('\n');
            }
            for (String resource : diff.changed()) {
                writeClass(run1Dir, resource, reference.get(resource));
                writeClass(runNDir, resource, current.get(resource));
                index.append("CHANGED ").append(resource)
                        .append(" run-1-sha256=").append(sha256(reference.get(resource)))
                        .append(" run-").append(run).append("-sha256=").append(sha256(current.get(resource))).append('\n');
            }

            decompile(run1Dir, baseDir.resolve("run-1-decompiled"), index);
            decompile(runNDir, baseDir.resolve("run-" + run + "-decompiled"), index);

            Files.writeString(baseDir.resolve("mismatch.txt"), index.toString());
            return baseDir;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to dump reproducibility mismatch to " + baseDir, e);
        }
    }

    private static void decompile(Path classDir, Path outputDir, StringBuilder index) throws IOException {
        if (!Files.exists(classDir)) {
            return;
        }
        Files.createDirectories(outputDir);
        ByteArrayOutputStream logBuffer = new ByteArrayOutputStream();
        try (PrintStream logStream = new PrintStream(logBuffer, true, StandardCharsets.UTF_8)) {
            BaseDecompiler decompiler = new BaseDecompiler(
                    new DirectoryResultSaver(outputDir), VINEFLOWER_OPTIONS, new PrintStreamLogger(logStream));
            decompiler.addSource(classDir.toFile());
            decompiler.decompileContext();
            index.append("DECOMPILED ").append(classDir).append(" -> ").append(outputDir).append('\n');
        } catch (Exception e) {
            index.append("DECOMPILE_FAILED ").append(classDir).append(" -> ").append(outputDir)
                    .append(" (").append(e.getClass().getSimpleName()).append(": ")
                    .append(Objects.toString(e.getMessage(), "no message")).append(")\n");
        } finally {
            Files.write(outputDir.resolve("decompile.log"), logBuffer.toByteArray());
        }
    }

    private static void writeClass(Path dir, String resource, byte[] data) throws IOException {
        Path file = dir.resolve(resource + ".class");
        Files.createDirectories(file.getParent());
        Files.write(file, data);
    }

    private static String sha256(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static final class DirectoryResultSaver implements IResultSaver {

        private final Path outputDir;

        DirectoryResultSaver(Path outputDir) {
            this.outputDir = outputDir;
        }

        @Override
        public void saveFolder(String path) {
            mkdirs(outputDir.resolve(path));
        }

        @Override
        public void copyFile(String source, String path, String entryName) {
            Path target = resolve(path, entryName);
            mkdirs(target.getParent());
            try {
                Files.copy(Path.of(source), target);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to copy " + source + " to " + target, e);
            }
        }

        @Override
        public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
            writeString(resolve(path, entryName), content);
        }

        @Override
        public void createArchive(String path, String archiveName, Manifest manifest) {
        }

        @Override
        public void saveDirEntry(String path, String archiveName, String entryName) {
            mkdirs(resolve(path, entryName));
        }

        @Override
        public void copyEntry(String source, String path, String archiveName, String entryName) {
            Path target = resolve(path, entryName);
            mkdirs(target.getParent());
            try {
                Files.copy(Path.of(source), target);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to copy " + source + " to " + target, e);
            }
        }

        @Override
        public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
            writeString(resolve(path, entryName), content);
        }

        @Override
        public void closeArchive(String path, String archiveName) {
        }

        private Path resolve(String path, String entryName) {
            return (path == null || path.isEmpty()) ? outputDir.resolve(entryName)
                    : outputDir.resolve(path).resolve(entryName);
        }

        private static void writeString(Path target, String content) {
            mkdirs(target.getParent());
            try {
                Files.writeString(target, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to write " + target, e);
            }
        }

        private static void mkdirs(Path dir) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create directory " + dir, e);
            }
        }
    }
}
