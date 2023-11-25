package org.jboss.resteasy.reactive.server.vertx.test.inputstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;

/**
 * Fake InputStream often used in various situation for testing:
 * It does not really allocate the space in memory so big InputStream can be tested.
 */
public class FakeInputStream extends InputStream {
    public static final int DEFAULT_BUFFER_SIZE = 131072;
    private static final int MAX_AVAILABLE = 1024 * 1024 * 100;
    private final byte b;
    protected long toSend;
    private final Random random;

    /**
     * Will generate a FakeInputStream with random values
     *
     * @param len the length of the virtual InputStream
     */
    public FakeInputStream(final long len) {
        toSend = len;
        this.b = 0;
        random = new Random(System.nanoTime());
    }

    /**
     * @param len the length of the virtual InputStream
     * @param b   the byte to use for each and every byte
     */
    public FakeInputStream(final long len, final byte b) {
        toSend = len;
        this.b = b;
        random = null;
    }

    /**
     * @param inputStream the InputStream to consume completely
     * @return the length read
     * @throws IOException if an error occurs
     */
    public static long consumeAll(final InputStream inputStream) throws IOException {
        long len = 0;
        int read;
        final var bytes = new byte[DEFAULT_BUFFER_SIZE];
        while ((read = inputStream.read(bytes, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            len += read;
        }
        return len;
    }

    /**
     * @param inputStream the InputStream to consume completely
     * @return the length read
     * @throws IOException if an error occurs
     */
    public static long consumeAllLog(final InputStream inputStream) throws IOException {
        long len = 0;
        int read;
        final var bytes = new byte[DEFAULT_BUFFER_SIZE];
        System.out.print("Start " + DEFAULT_BUFFER_SIZE);// NOSONAR intentional for logging
        while ((read = inputStream.read(bytes, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            len += read;
            System.out.print(".");// NOSONAR intentional for logging
            System.out.flush();// NOSONAR intentional for logging
        }
        System.out.println("Done");// NOSONAR intentional for logging
        System.out.flush();// NOSONAR intentional for logging
        return len;
    }

    @Override
    public int read() throws IOException {
        if (toSend <= 0) {
            return -1;
        }
        toSend--;
        if (random != null) {
            return random.nextInt() & 0xFF;
        }
        return b & 0xFF;
    }

    @Override
    public int read(final byte[] bytes) throws IOException {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }
        return read(bytes, 0, bytes.length);
    }

    @Override
    public int read(final byte[] bytes, final int off, final int len) throws IOException {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }
        if (toSend <= 0) {
            return -1;
        }
        final var read = (int) Math.min(len, toSend);
        if (random == null) {
            Arrays.fill(bytes, off, off + read, b);
        } else {
            random.nextBytes(bytes);
        }
        toSend -= read;
        return read;
    }

    @Override
    public long skip(final long n) throws IOException {
        final var read = Math.min(toSend, n);
        toSend -= read;
        return read;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(Math.min(MAX_AVAILABLE, toSend), DEFAULT_BUFFER_SIZE);
    }

    @Override
    public void close() throws IOException {
        toSend = -1;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public long transferTo(final OutputStream out) throws IOException {
        final var readFinal = toSend;
        final var bytes = new byte[DEFAULT_BUFFER_SIZE];
        if (random == null) {
            Arrays.fill(bytes, b);
        } else {
            random.nextBytes(bytes);
        }
        var read = (int) skip(DEFAULT_BUFFER_SIZE);
        while (read > 0) {
            out.write(bytes, 0, read);
            read = (int) skip(DEFAULT_BUFFER_SIZE);
        }
        return readFinal;
    }
}
