package io.quarkus.annotation.processor.fs;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CustomMemoryFileSystem extends FileSystem {

    private final Map<URI, ByteBuffer> fileContents = new HashMap<>();
    private final CustomMemoryFileSystemProvider provider;

    public CustomMemoryFileSystem(CustomMemoryFileSystemProvider provider) {
        this.provider = provider;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        // No resources to close
    }

    @Override
    public boolean isOpen() {
        return true; // Always open
    }

    @Override
    public boolean isReadOnly() {
        return false; // This filesystem is writable
    }

    @Override
    public String getSeparator() {
        return "/"; // Unix-style separator
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton(Paths.get("/")); // Single root directory
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.emptyList(); // No file stores
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.emptySet(); // No supported file attribute views
    }

    @Override
    public Path getPath(String first, String... more) {
        String path = first;
        for (String segment : more) {
            path += "/" + segment;
        }
        return Paths.get(path);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return null;
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return null;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return null;
    }

    public void addFile(URI uri, byte[] content) {
        fileContents.put(uri, ByteBuffer.wrap(content));
    }

    static class CustomMemorySeekableByteChannel implements SeekableByteChannel {

        private final ByteBuffer buffer;

        CustomMemorySeekableByteChannel(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int remaining = buffer.remaining();
            int count = Math.min(remaining, dst.remaining());
            if (count > 0) {
                ByteBuffer slice = buffer.slice();
                slice.limit(count);
                dst.put(slice);
                buffer.position(buffer.position() + count);
            }
            return count;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int count = src.remaining();
            buffer.put(src);
            return count;
        }

        @Override
        public long position() throws IOException {
            return buffer.position();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            buffer.position((int) newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            return buffer.limit();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            buffer.limit((int) size);
            return this;
        }

        @Override
        public boolean isOpen() {
            return true; // Always open
        }

        @Override
        public void close() throws IOException {
            // No resources to close
        }
    }

}
