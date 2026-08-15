package io.quarkus.aesh.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.aesh.terminal.AbstractConnection;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.BaseDevice;
import org.aesh.terminal.Device;
import org.aesh.terminal.EventDecoder;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.Parser;

/**
 * A {@link org.aesh.terminal.Connection} backed by JDK {@link InputStream}/{@link OutputStream} pairs.
 * <p>
 * Extends {@link AbstractConnection} to leverage its {@link EventDecoder} which
 * buffers input in a queue when no {@code stdinHandler} is set. This prevents
 * input loss during the window between readline cycles where the handler is
 * temporarily null (see <a href="https://github.com/aeshell/aesh-readline/issues/233">aesh-readline#233</a>).
 * <p>
 * Used by {@link CliRunner} when running in test mode. The test framework
 * provides the streams via {@link AeshTestConnectionHolder}, and this
 * class wraps them into a proper aesh Connection -- all within the
 * runtime classloader, avoiding cross-classloader type issues.
 */
class AeshStreamConnection extends AbstractConnection {

    private final Device device = new BaseDevice("test");
    private final Size size = new Size(120, 40);
    private final InputStream input;
    private final OutputStream output;

    private volatile boolean closed = false;
    private Thread readerThread;

    AeshStreamConnection(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
        this.attributes = new Attributes();
        this.eventDecoder = new EventDecoder(this.attributes);
        this.stdout = data -> {
            try {
                String text = Parser.fromCodePoints(data);
                output.write(text.getBytes(StandardCharsets.UTF_8));
                output.flush();
            } catch (IOException e) {
                // Connection closed
            }
        };
    }

    @Override
    public Device device() {
        return device;
    }

    @Override
    public Size size() {
        return size;
    }

    @Override
    public void close() {
        closed = true;
        // Close the input stream to unblock the reader thread
        try {
            input.close();
        } catch (IOException e) {
            // Ignore
        }
        if (closeHandler != null) {
            closeHandler.accept(null);
        }
    }

    @Override
    public void openBlocking() {
        startReader();
        try {
            if (readerThread != null) {
                readerThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void openNonBlocking() {
        startReader();
    }

    @Override
    public boolean put(Capability capability, Object... params) {
        return false;
    }

    @Override
    public Charset inputEncoding() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public Charset outputEncoding() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public boolean supportsAnsi() {
        return false;
    }

    private void startReader() {
        if (readerThread != null) {
            return;
        }
        readerThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            try {
                while (!closed) {
                    int n = input.read(buffer);
                    if (n == -1) {
                        break;
                    }
                    if (n > 0) {
                        String text = new String(buffer, 0, n, StandardCharsets.UTF_8);
                        // Deliver input via EventDecoder which buffers in its
                        // inputQueue when stdinHandler is null, preventing
                        // input loss between readline cycles.
                        eventDecoder.accept(Parser.toCodePoints(text));
                    }
                }
            } catch (IOException e) {
                // Stream closed, exit reader
            }
        }, "aesh-test-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }
}
