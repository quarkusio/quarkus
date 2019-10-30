package io.quarkus.platform.tools;

import java.io.PrintStream;

public class DefaultMessageWriter implements MessageWriter {

    protected final PrintStream out;
    protected boolean debug;

    public DefaultMessageWriter() {
        this(System.out);
    }

    public DefaultMessageWriter(PrintStream out) {
        this.out = out;
    }

    public DefaultMessageWriter setDebugEnabled(boolean debugEnabled) {
        this.debug = debugEnabled;
        return this;
    }

    @Override
    public boolean isDebugEnabled() {
        return debug;
    }

    @Override
    public void info(String msg) {
        out.println(msg);
    }

    @Override
    public void error(String msg) {
        out.println(msg);
    }

    @Override
    public void debug(String msg) {
        if(!isDebugEnabled()) {
            return;
        }
        out.println("DEBUG: " + msg);
    }

    @Override
    public void warn(String msg) {
        out.println("WARN: " + msg);
    }
}
