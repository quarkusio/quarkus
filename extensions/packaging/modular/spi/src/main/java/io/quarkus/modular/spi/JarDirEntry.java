package io.quarkus.modular.spi;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

final class JarDirEntry implements Closeable {
    private final JarOutputStream jarOutput;
    private final JarEntry entry;
    private boolean closed;

    JarDirEntry(JarOutputStream jarOutput, String name) throws IOException {
        if (!name.endsWith("/")) {
            throw new IllegalArgumentException("Directory name must end with /");
        }
        this.jarOutput = jarOutput;
        this.entry = new JarEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(0);
        entry.setCompressedSize(0);
        entry.setCrc(0);
        jarOutput.putNextEntry(entry);
    }

    public void close() throws IOException {
        if (!closed) {
            closed = true;
            jarOutput.closeEntry();
        }
    }

    public void setLastModifiedTime(final FileTime time) {
        entry.setLastModifiedTime(time);
    }

    public void setLastAccessTime(final FileTime time) {
        entry.setLastAccessTime(time);
    }

    public void setCreationTime(final FileTime time) {
        entry.setCreationTime(time);
    }
}
