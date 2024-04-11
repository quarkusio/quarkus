package io.quarkus.annotation.processor.fs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CustomMemoryFileSystemProvider extends FileSystemProvider {

    private static final String MEM = "mem";

    private static Map<URI, ByteBuffer> fileContents = new HashMap();

    public static void reset() {
        fileContents = new HashMap();
    }

    public static Set<URI> getCreatedFiles() {
        return fileContents.keySet();
    }

    @Override
    public String getScheme() {
        return MEM;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        // There's a bit of a disconnect here between the Elementary JavaFileManager and the memory filesystem,
        // even though both are in-memory filesystems
        return new CustomMemoryFileSystem(this);
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getPath(URI uri) {

        if (uri.getScheme() == null || !uri.getScheme()
                .equalsIgnoreCase(MEM)) {
            throw new IllegalArgumentException("For URI " + uri + ", URI scheme is not '" + MEM + "'");

        }

        // TODO what should we do here? Can we use the java file manager used by Elementary?
        try {
            return Path.of(File.createTempFile("mem-fs", "adhoc")
                    .toURI());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        if (fileContents.containsKey(path.toUri())) {
            ByteBuffer buffer = fileContents.get(path.toUri());
            return new CustomMemoryFileSystem.CustomMemorySeekableByteChannel(buffer);
        } else {
            throw new NoSuchFileException(path.toString());
        }
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameFile(Path path1, Path path2) throws IOException {
        return path1.equals(path2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        if (!fileContents.containsKey(path.toUri())) {
            throw new NoSuchFileException(path.toString());
        }
    }

    @Override
    public <V extends java.nio.file.attribute.FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
            LinkOption... options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }
}
