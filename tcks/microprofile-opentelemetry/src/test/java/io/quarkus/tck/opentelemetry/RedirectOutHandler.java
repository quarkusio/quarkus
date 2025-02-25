package io.quarkus.tck.opentelemetry;

import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class RedirectOutHandler extends Handler {
    private final PrintStream oldOut;
    private final PrintStream newOut;

    public RedirectOutHandler(final String pattern) throws Exception {
        if (pattern == null) {
            throw new IllegalArgumentException("file path pattern must not be null");
        }
        this.oldOut = System.out;
        this.newOut = new PrintStream(pattern);
        System.setOut(newOut);
    }

    @Override
    public void publish(final LogRecord record) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {
        newOut.close();
        System.setOut(oldOut);
    }

    private <T> T checkNotNullParam(String name, T value) throws IllegalArgumentException {
        checkNotNullParamChecked("name", name);
        checkNotNullParamChecked(name, value);
        return value;
    }

    private <T> void checkNotNullParamChecked(String name, T value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
