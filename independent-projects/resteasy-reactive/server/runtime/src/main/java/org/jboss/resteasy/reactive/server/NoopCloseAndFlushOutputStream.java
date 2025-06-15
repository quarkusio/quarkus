package org.jboss.resteasy.reactive.server;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is needed because some message writers don't give us a way to control if the output stream is going to be
 * closed or not.
 */
public class NoopCloseAndFlushOutputStream extends OutputStream {
    private final OutputStream delegate;

    public NoopCloseAndFlushOutputStream(OutputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }
}
