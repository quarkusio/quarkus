package io.quarkus.observability.promql.client.rest;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public class DebugInputStream extends FilterInputStream {
    private final Consumer<String> debugOutput;
    private final Charset charset;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public DebugInputStream(InputStream in, Consumer<String> debugOutput, Charset charset) {
        super(in);
        this.debugOutput = debugOutput;
        this.charset = charset;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b >= 0) {
            if (b == '\n') {
                var s = baos.toString(charset);
                baos.reset();
                debugOutput.accept(s);
            } else {
                baos.write(b);
            }
        }
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int nread = super.read(b, off, len);
        if (nread > 0) {
            int start = off;
            int end = off;
            while (end < off + nread) {
                if (b[end] == '\n') {
                    baos.write(b, start, end - start);
                    var s = baos.toString(charset);
                    baos.reset();
                    debugOutput.accept(s);
                    start = end + 1;
                    end = start;
                } else {
                    end++;
                }
            }
            if (end > start) {
                baos.write(b, start, end - start);
            }
        }
        return nread;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (baos.size() > 0) {
            var s = baos.toString(charset);
            baos.reset();
            debugOutput.accept(s);
        }
    }
}
