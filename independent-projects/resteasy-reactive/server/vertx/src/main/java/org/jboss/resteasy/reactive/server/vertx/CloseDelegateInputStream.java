package org.jboss.resteasy.reactive.server.vertx;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class CloseDelegateInputStream extends InputStream {

    final InputStream delegate;
    final Closeable closeable;

    public CloseDelegateInputStream(InputStream delegate, Closeable closeable) {
        this.delegate = delegate;
        this.closeable = closeable;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int ret = delegate.read(b);
        if (ret == -1) {
            delegate.close();
        }
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = delegate.read(b, off, len);
        if (ret == -1) {
            delegate.close();
        }
        return ret;
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } finally {
            closeable.close();
        }
    }

    @Override
    public int read() throws IOException {
        int ret = delegate.read();
        if (ret == -1) {
            delegate.close();
        }
        return ret;
    }
}
