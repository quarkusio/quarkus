package io.quarkus.vertx.http.runtime;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Wraps an exception and prints stack trace with code snippets.
 *
 * <p>
 * To obtain the original exception, use {@link #getOriginal()}.
 *
 * <p>
 * NOTE: Copied from <a href="https://github.com/laech/java-stacksrc">laech/java-stacksrc</a>
 */
final class DecoratedAssertionError extends AssertionError {

    private final Throwable original;
    private final String decoratedStackTrace;

    DecoratedAssertionError(Throwable original) {
        this(original, null);
    }

    /**
     * @param pruneStackTraceKeepFromClass if not null, will prune the stack traces, keeping only
     *        elements that are called directly or indirectly by this class
     */
    DecoratedAssertionError(
            Throwable original, Class<?> pruneStackTraceKeepFromClass) {
        this.original = original;
        this.decoratedStackTrace = StackTraceDecorator.get().decorate(original, pruneStackTraceKeepFromClass);
        setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public String getMessage() {
        // Override this instead of calling the super(message) constructor, as super(null) will create
        // the "null" string instead of actually being null
        return getOriginal().getMessage();
    }

    /** Gets the original throwable being wrapped. */
    public Throwable getOriginal() {
        return original;
    }

    @Override
    public void printStackTrace(PrintWriter out) {
        out.println(this);
    }

    @Override
    public void printStackTrace(PrintStream out) {
        out.println(this);
    }

    @Override
    public String toString() {
        return decoratedStackTrace;
    }
}
