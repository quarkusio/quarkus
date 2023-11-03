package io.quarkus.bootstrap.logging;

import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.LogContextInitializer;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;

import io.quarkus.bootstrap.graal.ImageInfo;

/**
 *
 */
public final class InitialConfigurator implements LogContextInitializer {

    public static final QuarkusDelayedHandler DELAYED_HANDLER = new QuarkusDelayedHandler();
    private static final Level MIN_LEVEL;

    private static final String SYS_PROP_NAME = "logging.initial-configurator.min-level";

    static {
        Level minLevel = Level.ALL;
        String minLevelSysProp = System.getProperty(SYS_PROP_NAME);
        if (minLevelSysProp != null) {
            try {
                minLevel = Level.parse(minLevelSysProp);
            } catch (IllegalArgumentException ignored) {
                throw new IllegalArgumentException(
                        String.format("Unable to convert %s (obtained from the %s system property) into a known logging level.",
                                minLevelSysProp, SYS_PROP_NAME));
            }
        }
        MIN_LEVEL = minLevel;
    }

    @Override
    public Level getMinimumLevel(final String loggerName) {
        return MIN_LEVEL;
    }

    @Override
    public Level getInitialLevel(final String loggerName) {
        return loggerName.isEmpty() ? Level.ALL : null;
    }

    @Override
    public Handler[] getInitialHandlers(final String loggerName) {
        if (loggerName.isEmpty()) {
            if (ImageInfo.inImageBuildtimeCode()) {
                // we can't set a cleanup filter without the build items ready
                return new Handler[] {
                        createDefaultHandler()
                };
            } else {
                return new Handler[] { DELAYED_HANDLER };
            }
        } else {
            return NO_HANDLERS;
        }
    }

    @Override
    public boolean useStrongReferences() {
        return true;
    }

    public static ConsoleHandler createDefaultHandler() {
        ConsoleHandler handler = new ConsoleHandler(new PatternFormatter("%d{HH:mm:ss,SSS} %-5p [%c{3.}] %s%e%n"));
        handler.setLevel(Level.INFO);
        return handler;
    }
}
