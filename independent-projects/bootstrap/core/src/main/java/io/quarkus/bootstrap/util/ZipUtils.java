package io.quarkus.bootstrap.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipError;

/**
 *
 * @author Alexey Loubyansky
 */
public class ZipUtils {

    private static final String JAR_URI_PREFIX = "jar:";
    private static final Map<String, String> CREATE_ENV = Collections.singletonMap("create", "true");

    public static void unzip(Path zipFile, Path targetDir) throws IOException {
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
        } catch (FileAlreadyExistsException fae) {
            throw new IOException("Could not create directory '" + targetDir + "' as a file already exists with the same name");
        }
        try (FileSystem zipfs = newFileSystem(zipFile)) {
            for (Path zipRoot : zipfs.getRootDirectories()) {
                copyFromZip(zipRoot, targetDir);
            }
        } catch (IOException | ZipError ioe) {
            // TODO: (at a later date) Get rid of the ZipError catching (and instead only catch IOException)
            //  since it's a JDK bug which threw the undeclared ZipError instead of an IOException.
            //  Java 9 fixes it https://bugs.openjdk.java.net/browse/JDK-8062754

            throw new IOException("Could not unzip " + zipFile + " to target dir " + targetDir, ioe);
        }
    }

    public static URI toZipUri(Path zipFile) throws IOException {
        URI zipUri = zipFile.toUri();
        try {
            zipUri = new URI(JAR_URI_PREFIX + zipUri.getScheme(), zipUri.getPath(), null);
        } catch (URISyntaxException e) {
            throw new IOException("Failed to create a JAR URI for " + zipFile, e);
        }
        return zipUri;
    }

    public static void copyFromZip(Path source, Path target) throws IOException {
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                        final Path targetDir = target.resolve(source.relativize(dir).toString());
                        try {
                            Files.copy(dir, targetDir);
                        } catch (FileAlreadyExistsException e) {
                             if (!Files.isDirectory(targetDir))
                                 throw e;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                        Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    public static void zip(Path src, Path zipFile, Instant entryTime) throws IOException {
        try (FileSystem zipfs = newZip(zipFile, entryTime)) {
            if(Files.isDirectory(src)) {
                try (Stream<Path> stream = Files.walk(src)) {
                    stream
                        .sorted() // sort the input paths to get a reproducible output
                        .forEach(srcPath -> {
                            final Path targetPath = zipfs.getPath(src.relativize(srcPath).toString());
                            try {
                                if (Files.isDirectory(srcPath)) {
                                    try {
                                        Files.copy(srcPath, targetPath);
                                    } catch (FileAlreadyExistsException e) {
                                         if (!Files.isDirectory(targetPath)) {
                                             throw e;
                                         }
                                    }
                                } else {
                                    Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(String.format("Could not copy from %s into ZIP file %s", srcPath, zipFile));
                            }
                        });
                }
            } else {
                Files.copy(src, zipfs.getPath(src.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static FileSystem newZip(Path zipFile, Instant entryTime) throws IOException {
        final Map<String, ?> env;
        if (Files.exists(zipFile)) {
            env = Collections.emptyMap();
        } else {
            env = CREATE_ENV;
            // explicitly create any parent dirs, since the ZipFileSystem only creates a new file
            // with "create" = "true", but doesn't create any parent dirs.

            // It's OK to not check the existence of the parent dir(s) first, since the API,
            // as per its contract doesn't throw any exception if the parent dir(s) already exist
            Files.createDirectories(zipFile.getParent());
        }
        return new ReproducibleZipFileSystem(newFileSystem(toZipUri(zipFile), env), entryTime);
    }

    /**
     * This call is not thread safe, a single of FileSystem can be created for the
     * profided uri until it is closed.
     * @param uri The uri to the zip file.
     * @param env Env map.
     * @return A new FileSystem.
     * @throws IOException  in case of a failure
     */
    public static FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        // If Multi threading required, logic should be added to wrap this fs
        // onto a fs that handles a reference counter and close the fs only when all thread are done
        // with it.
        try {
            return FileSystems.newFileSystem(uri, env);
        } catch (IOException | ZipError ioe) {
            // TODO: (at a later date) Get rid of the ZipError catching (and instead only catch IOException)
            //  since it's a JDK bug which threw the undeclared ZipError instead of an IOException.
            //  Java 9 fixes it https://bugs.openjdk.java.net/browse/JDK-8062754

            // include the URI for which the filesystem creation failed
            throw new IOException("Failed to create a new filesystem for " + uri, ioe);
        }
    }

    /**
     * This call is thread safe, a new FS is created for each invocation.
     * @param path The zip file.
     * @return A new FileSystem instance
     * @throws IOException  in case of a failure
     */
     public static FileSystem newFileSystem(final Path path) throws IOException {
         try {
             return FileSystems.newFileSystem(path, (ClassLoader) null);
         } catch (IOException | ZipError ioe) {
             // TODO: (at a later date) Get rid of the ZipError catching (and instead only catch IOException)
             //  since it's a JDK bug which threw the undeclared ZipError instead of an IOException.
             //  Java 9 fixes it https://bugs.openjdk.java.net/browse/JDK-8062754

             // include the path for which the filesystem creation failed
             throw new IOException("Failed to create a new filesystem for " + path, ioe);
         }
     }

    /**
     * This is a hack to get past the <a href="https://bugs.openjdk.java.net/browse/JDK-8232879">JDK-8232879</a>
     * issue which causes CRC errors when writing out data to (jar) files using ZipFileSystem.
     * TODO: Get rid of this method as soon as JDK-8232879 gets fixed and released in a public version
     *
     * @param original The original outputstream which will be wrapped into a new outputstream
     *                 that delegates to this one.
     * @return
     */
    public static OutputStream wrapForJDK8232879(final OutputStream original) {
        return new OutputStream() {
            @Override
            public void write(final byte[] b) throws IOException {
                original.write(b);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                original.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                original.flush();
            }

            @Override
            public void close() throws IOException {
                original.close();
            }

            @Override
            public void write(final int b) throws IOException {
                // we call the 3 arg write(...) method here, instead
                // of the single arg one to bypass the JDK-8232879 issue
                final byte[] buf = new byte[1];
                buf[0] = (byte) (b & 0xff);
                this.write(buf, 0, 1);
            }
        };
    }

    /**
     * A wrapper delegating to another {@link FileSystem} instance that enforces {@link #entryTime} for every entry upon
     * {@link #close()}.
     */
    static class ReproducibleZipFileSystem extends FileSystem {
        private final FileSystem delegate;
        private final FileTime entryTime;

        public ReproducibleZipFileSystem(FileSystem delegate, Instant entryTime) {
            this.delegate = delegate;
            this.entryTime = FileTime.fromMillis(entryTime.toEpochMilli());;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public FileSystemProvider provider() {
            return delegate.provider();
        }

        @Override
        public void close() throws IOException {
            try {
                for (Path dir : delegate.getRootDirectories()) {
                    try (Stream<Path> stream = Files.walk(dir)) {
                        stream
                                .filter(path -> !"/".equals(path.toString())) // nothing to do for the root path
                                .forEach(path -> {
                                    final Path safePath;
                                    if (Files.isDirectory(path) && path.getFileName() != null) {
                                        final String fileName = path.getFileName().toString();
                                        safePath = path.resolveSibling(fileName.substring(0, fileName.length() - 1));
                                    } else {
                                        safePath = path;
                                    }
                                    try {
                                        Files.getFileAttributeView(safePath, BasicFileAttributeView.class)
                                                .setTimes(entryTime, entryTime, entryTime);;
                                    } catch (IOException e) {
                                        throw new RuntimeException(String.format("Could not set time attributes on %s", path), e);
                                    }
                                });
                    }
                }
            } finally {
                delegate.close();
            }
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public boolean isReadOnly() {
            return delegate.isReadOnly();
        }

        @Override
        public String getSeparator() {
            return delegate.getSeparator();
        }

        @Override
        public Iterable<Path> getRootDirectories() {
            return delegate.getRootDirectories();
        }

        @Override
        public Iterable<FileStore> getFileStores() {
            return delegate.getFileStores();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public Set<String> supportedFileAttributeViews() {
            return delegate.supportedFileAttributeViews();
        }

        @Override
        public Path getPath(String first, String... more) {
            return delegate.getPath(first, more);
        }

        @Override
        public PathMatcher getPathMatcher(String syntaxAndPattern) {
            return delegate.getPathMatcher(syntaxAndPattern);
        }

        @Override
        public UserPrincipalLookupService getUserPrincipalLookupService() {
            return delegate.getUserPrincipalLookupService();
        }

        @Override
        public WatchService newWatchService() throws IOException {
            return delegate.newWatchService();
        }
    }
}
