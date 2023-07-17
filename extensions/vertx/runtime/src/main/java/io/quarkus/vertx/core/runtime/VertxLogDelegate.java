package io.quarkus.vertx.core.runtime;

import java.util.logging.LogRecord;

import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

import io.vertx.core.spi.logging.LogDelegate;

public class VertxLogDelegate implements LogDelegate {
    private final Logger logger;

    public VertxLogDelegate(String name) {
        this.logger = Logger.getLogger(name);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARN);
    }

    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.DEBUG);
    }

    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.TRACE);
    }

    public void fatal(final Object message) {
        log(Level.FATAL, message);
    }

    public void fatal(final Object message, final Throwable t) {
        log(Level.FATAL, message, t);
    }

    public void error(final Object message) {
        log(Level.ERROR, message);
    }

    @Override
    public void error(Object message, Object... params) {
        log(Level.ERROR, message, null, params);
    }

    public void error(final Object message, final Throwable t) {
        log(Level.ERROR, message, t);
    }

    @Override
    public void error(Object message, Throwable t, Object... params) {
        log(Level.ERROR, message, t, params);
    }

    public void warn(final Object message) {
        log(Level.WARN, message);
    }

    @Override
    public void warn(Object message, Object... params) {
        log(Level.WARN, message, null, params);
    }

    public void warn(final Object message, final Throwable t) {
        log(Level.WARN, message, t);
    }

    @Override
    public void warn(Object message, Throwable t, Object... params) {
        log(Level.WARN, message, t, params);
    }

    public void info(final Object message) {
        log(Level.INFO, message);
    }

    @Override
    public void info(Object message, Object... params) {
        log(Level.INFO, message, null, params);
    }

    public void info(final Object message, final Throwable t) {
        log(Level.INFO, message, t);
    }

    @Override
    public void info(Object message, Throwable t, Object... params) {
        log(Level.INFO, message, t, params);
    }

    public void debug(final Object message) {
        log(Level.DEBUG, message);
    }

    @Override
    public void debug(Object message, Object... params) {
        log(Level.DEBUG, message, null, params);
    }

    public void debug(final Object message, final Throwable t) {
        log(Level.DEBUG, message, t);
    }

    @Override
    public void debug(Object message, Throwable t, Object... params) {
        log(Level.DEBUG, message, t, params);
    }

    public void trace(final Object message) {
        log(Level.TRACE, message);
    }

    @Override
    public void trace(Object message, Object... params) {
        log(Level.TRACE, message, null, params);
    }

    public void trace(final Object message, final Throwable t) {
        log(Level.TRACE, message, t);
    }

    @Override
    public void trace(Object message, Throwable t, Object... params) {
        log(Level.TRACE, message, t, params);
    }

    private void log(Level level, Object message) {
        log(level, message, null);
    }

    private void log(Level level, Object message, Throwable t, Object... params) {
        if (!logger.isLoggable(level)) {
            return;
        }
        String msg = (message == null) ? "NULL" : message.toString();
        LogRecord record = new LogRecord(level, msg);
        record.setLoggerName(logger.getName());
        if (t != null) {
            record.setThrown(t);
        } else if (params != null && params.length != 0 && params[params.length - 1] instanceof Throwable) {
            // The exception may be the last parameters (SLF4J uses this convention).
            // As the logger can be used in a SLF4J style, we need to check
            record.setThrown((Throwable) params[params.length - 1]);
        }
        record.setSourceClassName(null);
        record.setParameters(params);
        logger.log(record);
    }
}
