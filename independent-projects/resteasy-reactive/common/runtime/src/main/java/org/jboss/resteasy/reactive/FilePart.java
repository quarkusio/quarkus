package org.jboss.resteasy.reactive;

import java.io.File;

/**
 * Wrapper type representing a partial {@link File} object to be sent.
 */
public class FilePart {

    /**
     * The file to send
     */
    public final File file;

    /**
     * The starting byte of the file
     */
    public final long offset;

    /**
     * The number of bytes to send
     */
    public final long count;

    /**
     * Create a new partial {@link File} object.
     * 
     * @param file The file to send
     * @param offset The starting byte of the file (must be >= 0)
     * @param count The number of bytes to send (must be >= 0 and offset+count <= file size)
     */
    public FilePart(File file, long offset, long count) {
        if (!file.exists())
            throw new IllegalArgumentException("File does not exist: " + file);
        if (!file.isFile())
            throw new IllegalArgumentException("File is not a regular file: " + file);
        if (!file.canRead())
            throw new IllegalArgumentException("File cannot be read: " + file);
        if (offset < 0)
            throw new IllegalArgumentException("Offset (" + offset + ") must be >= 0: " + file);
        if (count < 0)
            throw new IllegalArgumentException("Count (" + count + ") must be >= 0: " + file);
        if ((offset + count) > file.length())
            throw new IllegalArgumentException(
                    "Offset + count (" + (offset + count) + ") larger than file size (" + file.length() + "): " + file);
        this.file = file;
        this.offset = offset;
        this.count = count;
    }
}
