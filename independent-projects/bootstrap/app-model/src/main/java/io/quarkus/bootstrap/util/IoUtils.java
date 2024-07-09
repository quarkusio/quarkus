package io.quarkus.bootstrap.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
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

    /**
     * Recursively delete the file or directory given by {@code root}.
     * The implementation will attempt to do so in a secure manner.
     * Any problems encountered will be logged at {@code DEBUG} level.
     *
     * @param root the root path (must not be {@code null})
     */
    public static void recursiveDelete(Path root) {
        log.debugf("Recursively delete path %s", root);
        if (root == null || !Files.exists(root)) {
            return;
        }
        try {
            if (Files.isDirectory(root)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
                    recursiveDelete(ds);
                }
                try {
                    Files.delete(root);
                } catch (IOException e) {
                    log.debugf(e, "Unable to delete directory %s", root);
                }
            } else {
                log.debugf("Delete file %s", root);
                try {
                    Files.delete(root);
                } catch (IOException e) {
                    log.debugf(e, "Unable to delete file %s", root);
                }
            }
        } catch (IOException e) {
            log.debugf(e, "Error recursively deleting directory");
        }
    }

    /**
     * Creates a new empty directory or empties an existing one.
     * Any problems encountered while emptying the directory will be logged at {@code DEBUG} level.
     *
     * @param dir directory
     * @throws IOException if creating or accessing the directory itself fails
     */
    public static void createOrEmptyDir(Path dir) throws IOException {
        log.debugf("Create or empty directory %s", dir);
        Objects.requireNonNull(dir);
        if (!Files.exists(dir)) {
            log.debugf("Directory %s does not exist, create directories", dir);
            Files.createDirectories(dir);
            return;
        }
        // recursively delete the *contents* of the directory, if any (keep the directory itself)
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            recursiveDelete(ds);
        }
    }

    private static void recursiveDelete(DirectoryStream<Path> ds) {
        if (ds instanceof SecureDirectoryStream<Path> sds) {
            // best, fastest, and most likely path for most OSes
            recursiveDeleteSecure(sds);
        } else {
            // this may not work well on e.g. NFS, so we avoid this path if possible
            for (Path p : ds) {
                recursiveDelete(p);
            }
        }
    }

    private static void recursiveDeleteSecure(SecureDirectoryStream<Path> sds) {
        for (Path p : sds) {
            Path file = p.getFileName();
            BasicFileAttributes attrs;
            try {
                attrs = sds.getFileAttributeView(file, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
                        .readAttributes();
            } catch (IOException e) {
                log.debugf(e, "Unable to query file type of %s", p);
                continue;
            }
            if (attrs.isDirectory()) {
                try {
                    try (SecureDirectoryStream<Path> nested = sds.newDirectoryStream(file)) {
                        recursiveDeleteSecure(nested);
                    }
                    sds.deleteDirectory(file);
                } catch (IOException e) {
                    log.debugf(e, "Unable to delete directory %s", p);
                }
            } else {
                // log the whole path, not the file name
                log.debugf("Delete file %s", p);
                try {
                    sds.deleteFile(file);
                } catch (IOException e) {
                    log.debugf(e, "Unable to delete file %s", p);
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

    /**
     * Read the contents of a file as a string.
     *
     * @param file the file to read (must not be {@code null})
     * @return the file content, as a string (not {@code null})
     * @throws IOException if an error occurs when reading the file
     * @deprecated Use {@link Files#readString(Path, Charset)} instead.
     */
    @Deprecated(forRemoval = true)
    public static String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    /**
     * Copy the input stream to the given output stream.
     * Calling this method is identical to calling {@code in.transferTo(out)}.
     *
     * @param out the output stream (must not be {@code null})
     * @param in the input stream (must not be {@code null})
     * @throws IOException if an error occurs during the copy
     * @see InputStream#transferTo(OutputStream)
     */
    public static void copy(OutputStream out, InputStream in) throws IOException {
        in.transferTo(out);
    }

    /**
     * Write a string to a file using UTF-8 encoding.
     * The file will be created if it does not exist, and truncated if it is not empty.
     *
     * @param file the file to write (must not be {@code null})
     * @param content the string to write to the file (must not be {@code null})
     * @throws IOException if an error occurs when writing the file
     */
    public static void writeFile(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

}
