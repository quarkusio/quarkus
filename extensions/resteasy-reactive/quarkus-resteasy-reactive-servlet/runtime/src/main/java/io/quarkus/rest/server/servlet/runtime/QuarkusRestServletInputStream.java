package io.quarkus.rest.server.servlet.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.servlet.http.HttpServletRequest;

public class QuarkusRestServletInputStream extends InputStream {
    ByteBuffer existingData;
    HttpServletRequest request;
    InputStream delegate;

    public QuarkusRestServletInputStream(ByteBuffer existingData, HttpServletRequest request) {
        if (existingData.remaining() > 0) {
            this.existingData = existingData;
        }
        this.request = request;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int read = read(b);
        if (read == -1) {
            return -1;
        }
        return b[0] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (existingData != null) {
            int read = Math.min(len, existingData.remaining());
            existingData.get(b, off, read);
            if (existingData.remaining() == 0) {
                existingData = null;
            }
            return read;
        }
        if (delegate == null) {
            delegate = request.getInputStream();
        }
        return delegate.read(b, off, len);
    }
}
