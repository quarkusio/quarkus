package io.quarkus.modular.spi;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.attribute.FileTime;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

final class JarEntryOutputStream extends OutputStream {
    private final JarOutputStream jarOutput;
    private final JarEntry entry;
    private final CRC32 crc = new CRC32();
    private final File tempFile;
    private final RandomAccessFile raf;
    private final OutputStream os;
    private long written;
    private boolean closed;

    JarEntryOutputStream(final JarOutputStream jarOutput, final String name, boolean compress, long estSize)
            throws IOException {
        if (name.endsWith("/")) {
            throw new IllegalArgumentException("Entry file name must not end with /");
        }
        this.jarOutput = jarOutput;
        entry = new JarEntry(name);
        if (compress) {
            // they do the work for us; write directly
            entry.setMethod(ZipEntry.DEFLATED);
            os = jarOutput;
            raf = null;
            tempFile = null;
            jarOutput.putNextEntry(entry);
        } else {
            // we have to precompute CRC, size etc.
            // this is because ZipOutputStream does not support setting general flag bit 3 for STORED entries;
            // this flag indicates that CRC and size info go into the data descriptor at the end instead.
            // this is due to PKZIP 2.04g for DOS only supporting this for STORED. that's from 1993...
            // but ZipInputStream also does not support it now, so it is what it is.
            if (estSize >= 0 && estSize < 8192) {
                tempFile = null;
                raf = null;
                os = new ByteArrayOutputStream((int) estSize);
            } else {
                tempFile = File.createTempFile("quarkus-", "");
                raf = new RandomAccessFile(tempFile, "rw");
                try {
                    os = new FileOutputStream(raf.getFD());
                } catch (IOException e) {
                    try {
                        raf.close();
                    } catch (IOException e2) {
                        e.addSuppressed(e2);
                    }
                    throw e;
                }
            }
            entry.setMethod(ZipEntry.STORED);
        }
    }

    JarEntryOutputStream(final JarOutputStream jarOutput, final String name) throws IOException {
        this(jarOutput, name, false, -1);
    }

    public void write(final byte[] b) throws IOException {
        check();
        os.write(b);
        crc.update(b);
        written += b.length;
    }

    public void write(final byte[] b, final int off, final int len) throws IOException {
        check();
        os.write(b, off, len);
        crc.update(b, off, len);
        written += len;
    }

    public void write(final int b) throws IOException {
        check();
        os.write(b);
        crc.update(b);
        written++;
    }

    public void flush() throws IOException {
        check();
    }

    public void close() throws IOException {
        if (!closed) {
            closed = true;
            if (entry.getMethod() == ZipEntry.STORED) {
                try (Closeable ignored = jarOutput::closeEntry) {
                    if (raf != null) {
                        // it's in a temp file
                        try (os) {
                            try (raf) {
                                try (Closeable ignored2 = tempFile::delete) {
                                    entry.setCrc(crc.getValue());
                                    entry.setSize(written);
                                    entry.setCompressedSize(written);
                                    jarOutput.putNextEntry(entry);
                                    raf.seek(0);
                                    try (FileInputStream is = new FileInputStream(raf.getFD())) {
                                        is.transferTo(jarOutput);
                                    }
                                }
                            }
                        }
                    } else {
                        entry.setCrc(crc.getValue());
                        entry.setSize(written);
                        entry.setCompressedSize(written);
                        jarOutput.putNextEntry(entry);
                        jarOutput.write(((ByteArrayOutputStream) os).toByteArray());
                    }
                }
            } else {
                jarOutput.closeEntry();
            }
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

    private void check() throws IOException {
        if (closed) {
            throw new IOException("Entry closed");
        }
    }
}
