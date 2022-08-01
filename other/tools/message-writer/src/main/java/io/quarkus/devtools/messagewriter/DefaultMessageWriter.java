package io.quarkus.devtools.messagewriter;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.messagewriter.MessageIcons.WARN_ICON;

import java.io.PrintStream;

final class DefaultMessageWriter implements MessageWriter {

    protected final PrintStream out;
    protected boolean debug;

    DefaultMessageWriter() {
        this(System.out, false);
    }

    DefaultMessageWriter(PrintStream out) {
        this(out, false);
    }

    DefaultMessageWriter(boolean debug) {
        this(System.out, debug);
    }

    DefaultMessageWriter(PrintStream out, boolean debug) {
        this.out = out;
        this.debug = debug;
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
        out.println(ERROR_ICON + " " + msg);
    }

    @Override
    public void debug(String msg) {
        if (!isDebugEnabled()) {
            return;
        }
        out.println("[DEBUG] " + msg);
    }

    @Override
    public void warn(String msg) {
        out.println(WARN_ICON + " " + msg);
    }
}
