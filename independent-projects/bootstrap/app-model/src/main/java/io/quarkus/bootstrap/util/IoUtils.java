package io.quarkus.bootstrap.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;

import org.jboss.logging.Logger;

/**
 *
 * @author Alexey Loubyansky
 */
public class IoUtils {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private static final Path TMP_DIR = Paths.get(PropertyUtils.getProperty("java.io.tmpdir"));

    private static final Logger log = Logger.getLogger(IoUtils.class);

    private static void failedToMkDir(final Path dir) {
        throw new IllegalStateException("Failed to create directory " + dir);
    }

    public static Path createTmpDir(String name) {
        return mkdirs(TMP_DIR.resolve(name));
    }

    public static Path createRandomTmpDir() {
        return createTmpDir(UUID.randomUUID().toString());
    }

    public static Path createRandomDir(Path parentDir) {
        return mkdirs(parentDir.resolve(UUID.randomUUID().toString()));
    }

    public static Path mkdirs(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            failedToMkDir(dir);
        }
        return dir;
    }

    public static void recursiveDelete(Path root) {
        log.debugf("Recursively delete directory %s", root);
        if (root == null || !Files.exists(root)) {
            return;
        }
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    try {
                        Files.delete(file);
                    } catch (IOException ex) {
                        log.debugf(ex, "Unable to delete file " + file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                        throws IOException {
                    if (e == null) {
                        try {
                            Files.delete(dir);
                        } catch (IOException ex) {
                            log.debugf(ex, "Unable to delete directory " + dir);
                        }
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        throw e;
                    }
                }
            });
        } catch (IOException e) {
            log.debugf(e, "Error recursively deleting directory");
        }
    }

    /**
     * Creates a new empty directory or empties an existing one.
     *
     * @param dir directory
     * @throws IOException in case of a failure
     */
    public static void createOrEmptyDir(Path dir) throws IOException {
        log.debugf("Create or empty directory %s", dir);
        Objects.requireNonNull(dir);
        if (!Files.exists(dir)) {
            log.debugf("Directory %s does not exist, create directories", dir);
            Files.createDirectories(dir);
            return;
        }
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException(dir + " is not a directory");
        }
        log.debugf("Iterate over contents of %s to delete its contents", dir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    recursiveDelete(p);
                } else {
                    log.debugf("Delete file %s", p);
                    Files.delete(p);
                }
            }
        }
    }

    public static Path copy(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                throws IOException {
                            final Path targetDir = target.resolve(source.relativize(dir).toString());
                            try {
                                Files.copy(dir, targetDir);
                            } catch (FileAlreadyExistsException e) {
                                if (!Files.isDirectory(targetDir)) {
                                    throw e;
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException {
                            Files.copy(file, target.resolve(source.relativize(file).toString()),
                                    StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } else {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    public static String readFile(Path file) throws IOException {
        final char[] charBuffer = new char[DEFAULT_BUFFER_SIZE];
        int n = 0;
        final StringWriter output = new StringWriter();
        try (BufferedReader input = Files.newBufferedReader(file)) {
            while ((n = input.read(charBuffer)) != -1) {
                output.write(charBuffer, 0, n);
            }
        }
        return output.getBuffer().toString();
    }

    public static void copy(OutputStream out, InputStream in) throws IOException {
        in.transferTo(out);
    }

    public static void writeFile(Path file, String content) throws IOException {
        Files.write(file, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
    }

}
