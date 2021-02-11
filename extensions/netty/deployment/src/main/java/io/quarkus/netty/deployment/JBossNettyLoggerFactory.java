package io.quarkus.netty.deployment;

import org.jboss.logging.Logger;

import io.netty.util.internal.logging.AbstractInternalLogger;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class JBossNettyLoggerFactory extends InternalLoggerFactory {

    @Override
    protected InternalLogger newInstance(String name) {
        return new JBossNettyInternalLogger(name);
    }

    private static final class JBossNettyInternalLogger extends AbstractInternalLogger {

        final Logger log;

        JBossNettyInternalLogger(String name) {
            super(name);
            log = Logger.getLogger(name);
        }

        @Override
        public boolean isTraceEnabled() {
            return log.isTraceEnabled();
        }

        @Override
        public void trace(String msg) {
            log.trace(msg);
        }

        @Override
        public void trace(String format, Object arg) {
            log.tracef(format, arg);
        }

        @Override
        public void trace(String format, Object argA, Object argB) {
            log.tracef(format, argA, argB);
        }

        @Override
        public void trace(String format, Object... arguments) {
            log.tracef(format, arguments);
        }

        @Override
        public void trace(String msg, Throwable t) {
            log.trace(msg, t);
        }

        @Override
        public boolean isDebugEnabled() {
            return log.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            log.debug(msg);
        }

        @Override
        public void debug(String format, Object arg) {
            log.debugf(format, arg);
        }

        @Override
        public void debug(String format, Object argA, Object argB) {
            log.debugf(format, argA, argB);
        }

        @Override
        public void debug(String format, Object... arguments) {
            log.debugf(format, arguments);
        }

        @Override
        public void debug(String msg, Throwable t) {
            log.debug(msg, t);
        }

        @Override
        public boolean isInfoEnabled() {
            return log.isInfoEnabled();
        }

        @Override
        public void info(String msg) {
            log.info(msg);
        }

        @Override
        public void info(String format, Object arg) {
            log.infof(format, arg);
        }

        @Override
        public void info(String format, Object argA, Object argB) {
            log.infof(format, argA, argB);
        }

        @Override
        public void info(String format, Object... arguments) {
            log.infof(format, arguments);
        }

        @Override
        public void info(String msg, Throwable t) {
            log.info(msg, t);
        }

        @Override
        public boolean isWarnEnabled() {
            return log.isEnabled(Logger.Level.WARN);
        }

        @Override
        public void warn(String msg) {
            log.warn(msg);
        }

        @Override
        public void warn(String format, Object arg) {
            log.warnf(format, arg);
        }

        @Override
        public void warn(String format, Object... arguments) {
            log.warnf(format, arguments);
        }

        @Override
        public void warn(String format, Object argA, Object argB) {
            log.warnf(format, argA, argB);
        }

        @Override
        public void warn(String msg, Throwable t) {
            log.warn(msg, t);
        }

        @Override
        public boolean isErrorEnabled() {
            return log.isEnabled(Logger.Level.ERROR);
        }

        @Override
        public void error(String msg) {
            log.error(msg);
        }

        @Override
        public void error(String format, Object arg) {
            log.errorf(format, arg);
        }

        @Override
        public void error(String format, Object argA, Object argB) {
            log.errorf(format, argA, argB);
        }

        @Override
        public void error(String format, Object... arguments) {
            log.errorf(format, arguments);
        }

        @Override
        public void error(String msg, Throwable t) {
            log.error(msg, t);
        }

    }

}
