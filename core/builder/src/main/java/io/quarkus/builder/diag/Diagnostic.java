package io.quarkus.builder.diag;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import io.quarkus.builder.location.Location;
import io.smallrye.common.constraint.Assert;

public final class Diagnostic {
    private final Level level;
    private final Location location;
    private final String format;
    private final Object[] args;
    private final Throwable thrown;

    public Diagnostic(final Level level, final Location location, final String format, final Object... args) {
        this(level, null, location, format, args);
    }

    public Diagnostic(final Level level, final Throwable thrown, final Location location, final String format,
            final Object... args) {
        Assert.checkNotNullParam("level", level);
        Assert.checkNotNullParam("format", format);
        Assert.checkNotNullParam("args", args);
        this.level = level;
        this.location = location;
        this.format = format;
        this.args = args.clone();
        this.thrown = thrown;
    }

    public void print(PrintStream os) {
        if (location != null) {
            os.print(location);
            os.print(": ");
        }
        os.print('[');
        os.print(level);
        os.print("]: ");
        os.printf(format, args);
        if (thrown != null) {
            os.print(": ");
            thrown.printStackTrace(os);
        }
        os.println();
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(final StringBuilder b) {
        if (location != null) {
            b.append(location).append(": ");
        }
        b.append('[').append(level).append("]: ");
        b.append(String.format(format, args));
        if (thrown != null) {
            Writer stacktraceWriter = new StringWriter();
            thrown.printStackTrace(new PrintWriter(stacktraceWriter));
            b.append(": ").append(stacktraceWriter.toString());
        }
        return b;
    }

    public Throwable getThrown() {
        return thrown;
    }

    public Level getLevel() {
        return level;
    }

    public enum Level {
        ERROR("error"),
        WARN("warning"),
        NOTE("note"),
        ;

        private final String name;

        Level(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
