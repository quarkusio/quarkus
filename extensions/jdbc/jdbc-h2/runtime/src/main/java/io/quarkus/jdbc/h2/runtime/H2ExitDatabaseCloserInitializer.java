package io.quarkus.jdbc.h2.runtime;

import io.quarkus.runtime.util.IsolatedClassInitializer;

/**
 * Initializes, in isolation, the H2 classes whose static initializers capture the application class loader.
 * <p>
 * In dev and test mode, H2 is shared across application restarts while the application class loaders are closed
 * on restart. {@code OnExitDatabaseCloser} creates its shutdown hook thread in its static initializer, and that
 * thread inherits the context class loader of the thread that first opens a database. {@code DbException} and
 * {@code TraceObject} store pre-allocated exceptions in static fields, whose backtraces reference the classes of
 * every frame on the stack when they were created. Initialized from an application thread, they would retain a
 * closed application class loader for the rest of the JVM's life.
 *
 * @see IsolatedClassInitializer
 */
public final class H2ExitDatabaseCloserInitializer {

    private H2ExitDatabaseCloserInitializer() {
    }

    public static void init() {
        IsolatedClassInitializer.initialize(org.h2.Driver.class.getClassLoader(),
                "org.h2.engine.OnExitDatabaseCloser",
                "org.h2.message.DbException",
                "org.h2.message.TraceObject");
    }
}
