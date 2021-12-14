package io.quarkus.deployment.console;

import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Point;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;

/**
 * Fake connection that can be used to stop a running aesh instance
 *
 * If you create a console with one of these and then close it Aesh will assume the program is done and exit
 */
public class DelegateConnection implements Connection {

    private final Connection delegate;
    private Consumer<Size> sizeHandler;
    private Consumer<Signal> signalHandler;
    private Consumer<Void> closeHandler;
    private Consumer<int[]> stdinHandler;
    private Consumer<int[]> stdoutHandler;
    volatile boolean closed;

    public DelegateConnection(Connection delegate) {
        this.delegate = delegate;
    }

    @Override
    public Device device() {
        return delegate.device();
    }

    @Override
    public Size size() {
        return delegate.size();
    }

    @Override
    public Consumer<Size> getSizeHandler() {
        return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Size> consumer) {
        sizeHandler = consumer;
    }

    @Override
    public Consumer<Signal> getSignalHandler() {
        return signalHandler;
    }

    @Override
    public void setSignalHandler(Consumer<Signal> consumer) {
        this.signalHandler = consumer;
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
        return stdinHandler;
    }

    @Override
    public void setStdinHandler(Consumer<int[]> consumer) {
        this.stdinHandler = consumer;
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
        if (stdoutHandler == null) {
            return delegate.stdoutHandler();
        }
        return stdoutHandler;
    }

    @Override
    public void setCloseHandler(Consumer<Void> consumer) {
        this.closeHandler = consumer;
    }

    @Override
    public Consumer<Void> getCloseHandler() {
        return closeHandler;
    }

    @Override
    public void close() {
        closed = true;
        if (stdinHandler != null) {
            stdinHandler.accept(new int[] { -1 });
        }
    }

    @Override
    public void openBlocking() {

    }

    @Override
    public void openNonBlocking() {

    }

    @Override
    public boolean put(Capability capability, Object... objects) {
        if (closed) {
            return false;
        }
        return delegate.put(capability, objects);
    }

    @Override
    public Attributes getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public void setAttributes(Attributes attributes) {
        if (!closed) {
            delegate.setAttributes(attributes);
        }
    }

    @Override
    public Charset inputEncoding() {
        return delegate.inputEncoding();
    }

    @Override
    public Charset outputEncoding() {
        return delegate.outputEncoding();
    }

    @Override
    public boolean supportsAnsi() {
        return delegate.supportsAnsi();
    }

    @Override
    public void close(int exit) {
        close();
    }

    @Override
    public Connection write(String s) {
        if (!closed) {
            delegate.write(s);
        } else {
            System.out.println(s);
        }
        return this;
    }

    @Override
    public Attributes enterRawMode() {
        if (!closed) {
            return delegate.enterRawMode();
        } else {
            return delegate.getAttributes();
        }
    }

    @Override
    public Point getCursorPosition() {
        return delegate.getCursorPosition();
    }
}
