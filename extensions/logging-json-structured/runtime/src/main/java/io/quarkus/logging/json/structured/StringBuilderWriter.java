package io.quarkus.logging.json.structured;

import java.io.Writer;

final public class StringBuilderWriter extends Writer {

    private final StringBuilder builder;

    public StringBuilderWriter() {
        this(new StringBuilder());
    }

    public StringBuilderWriter(final StringBuilder builder) {
        this.builder = builder;
    }

    /**
     * Clears the builder used for the writer.
     *
     * @see StringBuilder#setLength(int)
     */
    void clear() {
        builder.setLength(0);
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        builder.append(cbuf, off, len);
    }

    @Override
    public void write(final int c) {
        builder.append((char) c);
    }

    @Override
    public void write(final char[] cbuf) {
        builder.append(cbuf);
    }

    @Override
    public void write(final String str) {
        builder.append(str);
    }

    @Override
    public void write(final String str, final int off, final int len) {
        builder.append(str, off, len);
    }

    @Override
    public Writer append(final CharSequence csq) {
        builder.append(csq);
        return this;
    }

    @Override
    public Writer append(final CharSequence csq, final int start, final int end) {
        builder.append(csq, start, end);
        return this;
    }

    @Override
    public Writer append(final char c) {
        builder.append(c);
        return this;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
