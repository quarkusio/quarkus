package io.quarkus.analytics.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;

/**
 * Reads a single line from the console in a way that stops as soon as the reading thread is interrupted.
 * <p>
 * A thread that keeps reading the console after its caller stopped waiting for an answer consumes input meant for
 * someone else, e.g. the dev mode console. How to stop a read depends on the stream, hence two variants:
 * <ul>
 * <li>{@link #readLine(InputStream)} for streams whose blocking {@code read()} cannot be interrupted, such as the
 * {@code System.in} of a plain JVM: it only reads bytes the stream reports as available.</li>
 * <li>{@link #readLineInterruptibly(InputStream)} for streams that report nothing as available but whose blocking
 * {@code read()} reacts to interrupts, such as the {@code System.in} that Gradle forwards to a build.</li>
 * </ul>
 * Both read byte by byte, so any input after the line is left in the stream for the next reader.
 */
public final class InterruptibleLineReader {

    private static final long POLL_INTERVAL_MILLIS = 20;

    private InterruptibleLineReader() {
    }

    /**
     * Reads one line, without the line terminator, never blocking in {@link InputStream#read()}.
     * <p>
     * Most streams report nothing as available once they reach their end, so a stream that ends without a line
     * terminator is usually indistinguishable from one waiting for more input: the call then returns only when the
     * thread is interrupted. Callers must therefore run it on a thread they interrupt after a timeout.
     *
     * @param in the stream to read from
     * @return the line, or {@code null} if the current thread is interrupted before a full line has been read
     *         (the interrupt status is preserved)
     * @throws IOException if reading from the stream fails
     */
    public static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            if (in.available() > 0) {
                int b = in.read();
                if (b == -1 || b == '\n') {
                    return toString(line);
                }
                line.write(b);
            } else {
                try {
                    Thread.sleep(POLL_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }

    /**
     * Reads one line, without the line terminator, with blocking reads that the given stream aborts when the
     * current thread is interrupted.
     * <p>
     * Only use it with such streams: with a stream whose read cannot be interrupted, the call keeps blocking after
     * the interrupt.
     *
     * @param in the stream to read from
     * @return the line, the bytes read so far at the end of the stream, or {@code null} if the current thread is
     *         interrupted before a full line has been read (the interrupt status is preserved)
     * @throws IOException if reading from the stream fails for another reason than an interrupt
     */
    public static String readLineInterruptibly(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            int b;
            try {
                b = in.read();
            } catch (IOException | RuntimeException e) {
                // streams report an interrupt in different ways, e.g. InterruptedIOException or an unchecked
                // wrapper of InterruptedException
                if (Thread.currentThread().isInterrupted() || isCausedByInterrupt(e)) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                throw e;
            }
            if (b == -1 || b == '\n') {
                return toString(line);
            }
            line.write(b);
        }
    }

    private static boolean isCausedByInterrupt(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof InterruptedException || t instanceof InterruptedIOException) {
                return true;
            }
        }
        return false;
    }

    private static String toString(ByteArrayOutputStream line) {
        String s = line.toString(Charset.defaultCharset());
        return s.endsWith("\r") ? s.substring(0, s.length() - 1) : s;
    }
}
