package io.quarkus.observability.promql.client.rest;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public class DebugOutputStream extends FilterOutputStream {
    private final Consumer<String> debugOutput;
    private final Charset charset;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public DebugOutputStream(OutputStream out, Consumer<String> debugOutput, Charset charset) {
        super(out);
        this.debugOutput = debugOutput;
        this.charset = charset;
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        if (b == '\n') {
            var s = baos.toString(charset);
            baos.reset();
            debugOutput.accept(s);
        } else {
            baos.write(b);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        int start = off;
        int end = off;
        while (end < off + len) {
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
