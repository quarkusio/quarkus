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
    public String implementation() {
        return "Quarkus";
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

    public void error(final Object message, final Throwable t) {
        log(Level.ERROR, message, t);
    }

    public void warn(final Object message) {
        log(Level.WARN, message);
    }

    public void warn(final Object message, final Throwable t) {
        log(Level.WARN, message, t);
    }

    public void info(final Object message) {
        log(Level.INFO, message);
    }

    public void info(final Object message, final Throwable t) {
        log(Level.INFO, message, t);
    }

    public void debug(final Object message) {
        log(Level.DEBUG, message);
    }

    public void debug(final Object message, final Throwable t) {
        log(Level.DEBUG, message, t);
    }

    public void trace(final Object message) {
        log(Level.TRACE, message);
    }

    public void trace(final Object message, final Throwable t) {
        log(Level.TRACE, message, t);
    }

    @Override
    public Object unwrap() {
        return logger;
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
