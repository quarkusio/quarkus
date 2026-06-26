package io.quarkus.aesh.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.BaseDevice;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.Parser;

/**
 * A {@link Connection} backed by JDK {@link InputStream}/{@link OutputStream} pairs.
 * <p>
 * Used by {@link CliRunner} when running in test mode. The test framework
 * provides the streams via {@link AeshTestConnectionHolder}, and this
 * class wraps them into a proper aesh Connection -- all within the
 * runtime classloader, avoiding cross-classloader type issues.
 */
class AeshStreamConnection implements Connection {

    private final Device device = new BaseDevice("test");
    private final Size size = new Size(120, 40);
    private final InputStream input;
    private final OutputStream output;

    private Consumer<Size> sizeHandler;
    private Consumer<Signal> signalHandler;
    private Consumer<int[]> stdinHandler;
    private Consumer<int[]> stdoutHandler;
    private Consumer<Void> closeHandler;
    private Attributes attributes;

    private volatile boolean closed = false;
    private Thread readerThread;

    AeshStreamConnection(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
        this.stdoutHandler = data -> {
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
    public Consumer<Size> sizeHandler() {
        return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Size> handler) {
        this.sizeHandler = handler;
    }

    @Override
    public Consumer<Signal> signalHandler() {
        return signalHandler;
    }

    @Override
    public void setSignalHandler(Consumer<Signal> handler) {
        this.signalHandler = handler;
    }

    @Override
    public Consumer<int[]> stdinHandler() {
        return stdinHandler;
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        this.stdinHandler = handler;
        // When the stdinHandler is set, start reading from the input stream
        if (handler != null && readerThread == null) {
            startReader();
        }
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
        return stdoutHandler;
    }

    @Override
    public void setCloseHandler(Consumer<Void> handler) {
        this.closeHandler = handler;
    }

    @Override
    public Consumer<Void> closeHandler() {
        return closeHandler;
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
        // Start reading and block until closed
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
    public Attributes attributes() {
        return attributes != null ? attributes : new Attributes();
    }

    @Override
    public void setAttributes(Attributes attr) {
        this.attributes = attr;
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
                    if (n > 0 && stdinHandler != null) {
                        String text = new String(buffer, 0, n, StandardCharsets.UTF_8);
                        stdinHandler.accept(Parser.toCodePoints(text));
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
