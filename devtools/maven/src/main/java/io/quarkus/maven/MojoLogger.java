package io.quarkus.maven;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.jboss.logging.Logger;
import org.jboss.logging.LoggerProvider;
import org.wildfly.common.Assert;

/**
 */
public class MojoLogger implements LoggerProvider {
    static final Object[] NO_PARAMS = new Object[0];

    public static volatile Supplier<Log> logSupplier;

    @Override
    public Logger getLogger(final String name) {
        return new Logger(name) {
            @Override
            protected void doLog(final Level level, final String loggerClassName, final Object message,
                    final Object[] parameters, final Throwable thrown) {
                final Supplier<Log> logSupplier = MojoLogger.logSupplier;
                if (logSupplier != null) {
                    Log log = logSupplier.get();
                    String text;
                    if (parameters == null || parameters.length == 0) {
                        text = String.valueOf(message);
                    } else {
                        try {
                            text = MessageFormat.format(String.valueOf(message), parameters);
                        } catch (Exception e) {
                            text = invalidFormat(String.valueOf(message), parameters);
                        }
                    }
                    synchronized (MojoLogger.class) {
                        doActualLog(log, level, text, thrown);
                    }
                }
            }

            @Override
            protected void doLogf(final Level level, final String loggerClassName, final String format,
                    final Object[] parameters, final Throwable thrown) {
                final Supplier<Log> logSupplier = MojoLogger.logSupplier;
                if (logSupplier != null) {
                    Log log = logSupplier.get();
                    String text;
                    if (parameters == null) {
                        try {
                            //noinspection RedundantStringFormatCall
                            text = String.format(format);
                        } catch (Exception e) {
                            text = invalidFormat(format, NO_PARAMS);
                        }
                    } else {
                        try {
                            text = String.format(format, (Object[]) parameters);
                        } catch (Exception e) {
                            text = invalidFormat(format, parameters);
                        }
                    }
                    synchronized (MojoLogger.class) {
                        doActualLog(log, level, text, thrown);
                    }
                }
            }

            @Override
            public boolean isEnabled(final Level level) {
                final Supplier<Log> logSupplier = MojoLogger.logSupplier;
                if (logSupplier == null)
                    return false;
                Log log = logSupplier.get();
                switch (level) {
                    case FATAL:
                    case ERROR:
                        return log.isErrorEnabled();
                    case WARN:
                        return log.isWarnEnabled();
                    case INFO:
                        return log.isInfoEnabled();
                    default:
                        return log.isDebugEnabled();
                }
            }

            void doActualLog(final Log log, final Level level, final String message, final Throwable thrown) {
                final MessageBuilder buffer = MessageUtils.buffer();
                // style options are limited unless we crack into jansi ourselves
                buffer.strong("[").project(name).strong("]").a(" ").a(message);
                if (thrown != null) {
                    switch (level) {
                        case FATAL:
                        case ERROR:
                            log.error(buffer.toString(), thrown);
                            break;
                        case WARN:
                            log.warn(buffer.toString(), thrown);
                            break;
                        case INFO:
                            log.info(buffer.toString(), thrown);
                            break;
                        default:
                            log.debug(buffer.toString(), thrown);
                            break;
                    }
                } else {
                    switch (level) {
                        case FATAL:
                        case ERROR:
                            log.error(buffer.toString());
                            break;
                        case WARN:
                            log.warn(buffer.toString());
                            break;
                        case INFO:
                            log.info(buffer.toString());
                            break;
                        default:
                            log.debug(buffer.toString());
                            break;
                    }
                }
            }
        };
    }

    String invalidFormat(final String format, final Object[] parameters) {
        final StringBuilder b = new StringBuilder("** invalid format \'" + format + "\'");
        if (parameters != null && parameters.length > 0) {
            b.append(" [").append(parameters[0]);
            for (int i = 1; i < parameters.length; i++) {
                b.append(',').append(parameters[i]);
            }
            b.append("]");
        }
        return b.toString();
    }

    @Override
    public void clearMdc() {
    }

    @Override
    public Object putMdc(final String key, final Object value) {
        //throw Assert.unsupported();
        return null;
    }

    @Override
    public Object getMdc(final String key) {
        return null;
    }

    @Override
    public void removeMdc(final String key) {
    }

    @Override
    public Map<String, Object> getMdcMap() {
        return Collections.emptyMap();
    }

    @Override
    public void clearNdc() {
    }

    @Override
    public String getNdc() {
        return "";
    }

    @Override
    public int getNdcDepth() {
        return 0;
    }

    @Override
    public String popNdc() {
        return "";
    }

    @Override
    public String peekNdc() {
        return "";
    }

    @Override
    public void pushNdc(final String message) {
        throw Assert.unsupported();
    }

    @Override
    public void setNdcMaxDepth(final int maxDepth) {
    }
}
