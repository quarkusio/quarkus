package io.quarkus.analytics.util;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Behaves like the {@code System.in} that Gradle forwards to a build: it never reports anything as available, and a
 * blocking read is aborted with an unchecked exception wrapping the {@link InterruptedException} when the reading
 * thread is interrupted.
 */
public class ForwardedInputStream extends InputStream {

    private final BlockingQueue<Integer> bytes = new LinkedBlockingQueue<>();

    public void type(String input) {
        for (byte b : input.getBytes()) {
            bytes.add(b & 0xFF);
        }
    }

    @Override
    public int read() {
        try {
            return bytes.take();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int available() {
        return 0;
    }
}
