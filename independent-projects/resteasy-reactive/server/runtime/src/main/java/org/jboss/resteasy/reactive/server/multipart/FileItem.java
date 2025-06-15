package org.jboss.resteasy.reactive.server.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Represents an item of a multipart message for which the {@code filename} attribute has been specified
 */
public interface FileItem {

    /**
     * Determines whether the body is held in memory
     */
    boolean isInMemory();

    /**
     * Gives access to the file stored on the file system. This should only be used when {@code isInMemory} is
     * {@code false}
     */
    Path getFile();

    /**
     * The size of the body - works regardless of the result of {@code isInMemory}
     */
    long getFileSize() throws IOException;

    /**
     * The body represented as an {@link InputStream} - works regardless of the result of {@code isInMemory}
     */
    InputStream getInputStream() throws IOException;

    /**
     * If the part represents a file on the file system, delete it, otherwise do nothing
     */
    void delete() throws IOException;

    /**
     * Copy the body to the specified {@link Path} - works regardless of the result of {@code isInMemory}
     */
    void write(Path target) throws IOException;
}
