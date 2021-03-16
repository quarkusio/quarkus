package io.quarkus.bootstrap.logging;

import io.quarkus.bootstrap.graal.ImageInfo;
import java.util.logging.Handler;
import java.util.logging.Level;
import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;

/**
 *
 */
public final class InitialConfigurator implements EmbeddedConfigurator {

    public static final QuarkusDelayedHandler DELAYED_HANDLER = new QuarkusDelayedHandler();

    @Override
    public Level getMinimumLevelOf(final String loggerName) {
        return Level.ALL;
    }

    @Override
    public Level getLevelOf(final String loggerName) {
        return loggerName.isEmpty() ? Level.ALL : null;
    }

    @Override
    public Handler[] getHandlersOf(final String loggerName) {
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
            return EmbeddedConfigurator.NO_HANDLERS;
        }
    }

    public static ConsoleHandler createDefaultHandler() {
        ConsoleHandler handler = new ConsoleHandler(new PatternFormatter("%d{HH:mm:ss,SSS} %-5p [%c{3.}] %s%e%n"));
        handler.setLevel(Level.INFO);
        return handler;
    }
}
