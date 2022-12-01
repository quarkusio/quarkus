package io.quarkus.netty.deployment;

import org.jboss.logging.Logger;

import io.netty.util.internal.logging.AbstractInternalLogger;
import io.netty.util.internal.logging.FormattingTuple;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.MessageFormatter;

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
            if (isTraceEnabled()) {
                log.trace(msg);
            }
        }

        @Override
        public void trace(String format, Object arg) {
            if (isTraceEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                trace0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void trace(String format, Object argA, Object argB) {
            if (isTraceEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                trace0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void trace(String format, Object... arguments) {
            if (isTraceEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                trace0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void trace(String msg, Throwable t) {
            if (isTraceEnabled()) {
                trace0(msg, t);
            }
        }

        private void trace0(String msg, Throwable t) {
            log.trace(msg, t);
        }

        @Override
        public boolean isDebugEnabled() {
            return log.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            if (isDebugEnabled()) {
                log.debug(msg);
            }
        }

        @Override
        public void debug(String format, Object arg) {
            if (isDebugEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                debug0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void debug(String format, Object argA, Object argB) {
            if (isDebugEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                debug0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void debug(String format, Object... arguments) {
            if (isDebugEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                debug0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void debug(String msg, Throwable t) {
            if (isDebugEnabled()) {
                debug0(msg, t);
            }
        }

        private void debug0(String msg, Throwable t) {
            log.debug(msg, t);
        }

        @Override
        public boolean isInfoEnabled() {
            return log.isInfoEnabled();
        }

        @Override
        public void info(String msg) {
            if (isInfoEnabled()) {
                log.info(msg);
            }
        }

        @Override
        public void info(String format, Object arg) {
            if (isInfoEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                info0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void info(String format, Object argA, Object argB) {
            if (isInfoEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                info0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void info(String format, Object... arguments) {
            if (isInfoEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                info0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void info(String msg, Throwable t) {
            if (isInfoEnabled()) {
                info0(msg, t);
            }
        }

        private void info0(String msg, Throwable t) {
            log.info(msg, t);
        }

        @Override
        public boolean isWarnEnabled() {
            return log.isEnabled(Logger.Level.WARN);
        }

        @Override
        public void warn(String msg) {
            if (isWarnEnabled()) {
                log.warn(msg);
            }
        }

        @Override
        public void warn(String format, Object arg) {
            if (isWarnEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                warn0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void warn(String format, Object... arguments) {
            if (isWarnEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                warn0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void warn(String format, Object argA, Object argB) {
            if (isWarnEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                warn0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void warn(String msg, Throwable t) {
            if (isWarnEnabled()) {
                warn0(msg, t);
            }
        }

        private void warn0(String msg, Throwable t) {
            log.warn(msg, t);
        }

        @Override
        public boolean isErrorEnabled() {
            return log.isEnabled(Logger.Level.ERROR);
        }

        @Override
        public void error(String msg) {
            if (isErrorEnabled()) {
                log.error(msg);
            }
        }

        @Override
        public void error(String format, Object arg) {
            if (isErrorEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                error0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void error(String format, Object argA, Object argB) {
            if (isErrorEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                error0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void error(String format, Object... arguments) {
            if (isErrorEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                error0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void error(String msg, Throwable t) {
            if (isErrorEnabled()) {
                error0(msg, t);
            }
        }

        private void error0(String msg, Throwable t) {
            log.error(msg, t);
        }

    }

}
