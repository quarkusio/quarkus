package io.quarkus.runtime.execution;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Optional;

/**
 * An execution context which is closeable, intended to emulate the ability to perform a shutdown action
 * for static initialization steps.
 */
public final class CloseableExecutionContext extends ExecutionContext implements AutoCloseable {
    private final AutoCloseable closeable;

    CloseableExecutionContext(final ExecutionContext parent, final AutoCloseable closeable) {
        super(parent);
        this.closeable = closeable;
    }

    public void close() throws Exception {
        Exception ex = null;
        try {
            closeable.close();
        } catch (Exception e) {
            ex = e;
        } catch (Throwable t) {
            ex = new UndeclaredThrowableException(t);
        }
        try {
            closeParent();
        } catch (Throwable t) {
            if (ex != null) {
                ex.addSuppressed(t);
                throw ex;
            } else {
                throw t;
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    private void closeParent() throws Exception {
        final Optional<CloseableExecutionContext> optParent = getParent().optionallyAs(CloseableExecutionContext.class);
        if (optParent.isPresent()) {
            optParent.get().close();
        }
    }
}
