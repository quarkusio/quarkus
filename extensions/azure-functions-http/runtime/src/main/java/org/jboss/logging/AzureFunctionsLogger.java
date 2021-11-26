package org.jboss.logging;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Copy/paste of final class JDKLogger to send log records towards the JDK logger from the Azure Functions Java Library.
 *
 * <p>
 * The class needs to be in org.jboss.logging package because the code uses classes that are package private.
 * </p>
 */
public class AzureFunctionsLogger extends Logger {
    private static final Logger log = Logger.getLogger("org.quarkus.azure.logging");

    protected AzureFunctionsLogger(String name) {
        super(name);
    }

    protected void doLog(final Level level, final String loggerClassName, final Object message, final Object[] parameters,
            final Throwable thrown) {
        if (isEnabled(level))
            try {
                final java.util.logging.Logger azureFunctionsLogger = AzureFunctionsLoggerProvider.getAzureFunctionsLogger();
                if (azureFunctionsLogger == null) {
                    // fall back -- if for any reason the azurefunction logger isn't set
                    // better log to standard out than nowhere
                    log.doLog(level, loggerClassName, message, parameters, thrown);
                } else {
                    final JBossLogRecord rec = new JBossLogRecord(translate(level), String.valueOf(message), loggerClassName);
                    if (thrown != null)
                        rec.setThrown(thrown);
                    rec.setLoggerName(getName());
                    rec.setParameters(parameters);
                    rec.setResourceBundleName(azureFunctionsLogger.getResourceBundleName());
                    rec.setResourceBundle(azureFunctionsLogger.getResourceBundle());
                    azureFunctionsLogger.log(rec);
                }
            } catch (Throwable ignored) {
            }
    }

    protected void doLogf(final Level level, final String loggerClassName, String format, final Object[] parameters,
            final Throwable thrown) {
        if (isEnabled(level))
            try {
                final java.util.logging.Logger azureFunctionsLogger = AzureFunctionsLoggerProvider.getAzureFunctionsLogger();
                if (azureFunctionsLogger == null) {
                    // fall back -- if for any reason the azurefunction logger isn't set
                    // better log to standard out than nowhere
                    log.doLogf(level, loggerClassName, format, parameters, thrown);
                } else {
                    final ResourceBundle resourceBundle = azureFunctionsLogger.getResourceBundle();
                    if (resourceBundle != null)
                        try {
                            format = resourceBundle.getString(format);
                        } catch (MissingResourceException e) {
                            // ignore
                        }
                    final String msg = parameters == null ? String.format(format) : String.format(format, parameters);
                    final JBossLogRecord rec = new JBossLogRecord(translate(level), msg, loggerClassName);
                    if (thrown != null)
                        rec.setThrown(thrown);
                    rec.setLoggerName(getName());
                    rec.setResourceBundleName(azureFunctionsLogger.getResourceBundleName());
                    // we've done all the business
                    rec.setResourceBundle(null);
                    rec.setParameters(null);
                    azureFunctionsLogger.log(rec);
                }
            } catch (Throwable ignored) {
            }
    }

    private static java.util.logging.Level translate(final Level level) {
        if (level == Level.TRACE) {
            return JDKLevel.TRACE;
        } else if (level == Level.DEBUG) {
            return JDKLevel.DEBUG;
        }
        return infoOrHigher(level);
    }

    private static java.util.logging.Level infoOrHigher(final Level level) {
        if (level == Level.INFO) {
            return JDKLevel.INFO;
        } else if (level == Level.WARN) {
            return JDKLevel.WARN;
        } else if (level == Level.ERROR) {
            return JDKLevel.ERROR;
        } else if (level == Level.FATAL) {
            return JDKLevel.FATAL;
        }
        return JDKLevel.ALL;
    }

    public boolean isEnabled(final Level level) {
        return AzureFunctionsLoggerProvider.getAzureFunctionsLogger() != null
                && AzureFunctionsLoggerProvider.getAzureFunctionsLogger().isLoggable(translate(level));
    }
}
