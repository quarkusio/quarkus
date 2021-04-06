package org.jboss.resteasy.reactive;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wrapper type representing the {@link Path} to a partial file object to be sent.
 */
public class PathPart {

    /**
     * The file to send
     */
    public final Path file;

    /**
     * The starting byte of the file
     */
    public final long offset;

    /**
     * The number of bytes to send
     */
    public final long count;

    /**
     * Create a new partial {@link Path} object.
     * 
     * @param path The file to send
     * @param offset The starting byte of the file (must be >= 0)
     * @param count The number of bytes to send (must be >= 0 and offset+count <= file size)
     */
    public PathPart(Path file, long offset, long count) {
        if (!Files.exists(file))
            throw new IllegalArgumentException("File does not exist: " + file);
        if (!Files.isRegularFile(file))
            throw new IllegalArgumentException("File is not a regular file: " + file);
        if (!Files.isReadable(file))
            throw new IllegalArgumentException("File cannot be read: " + file);
        if (offset < 0)
            throw new IllegalArgumentException("Offset (" + offset + ") must be >= 0: " + file);
        if (count < 0)
            throw new IllegalArgumentException("Count (" + count + ") must be >= 0: " + file);
        long fileLength;
        try {
            fileLength = Files.size(file);
            if ((offset + count) > fileLength)
                throw new IllegalArgumentException(
                        "Offset + count (" + (offset + count) + ") larger than file size (" + fileLength + "): " + file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.file = file;
        this.offset = offset;
        this.count = count;
    }
}
