package org.jboss.logging;

/**
 */
final class NoOpLogger extends Logger {
    static final NoOpLogger INSTANCE = new NoOpLogger();

    private NoOpLogger() {
        super("unnamed");
    }

    static Logger getInstance() {
        return INSTANCE;
    }

    protected void doLog(final Level level, final String loggerClassName, final Object message, final Object[] parameters, final Throwable thrown) {
    }

    protected void doLogf(final Level level, final String loggerClassName, final String format, final Object[] parameters, final Throwable thrown) {
    }

    public boolean isEnabled(final Level level) {
        return false;
    }
}
