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
     * Gives access to the file stored on the file system. When {@code isInMemory} is {@code true}, the content is
     * written to a temporary file on the first call and {@code isInMemory} returns {@code false} from then on, so this
     * should only be used when a file is actually needed
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
