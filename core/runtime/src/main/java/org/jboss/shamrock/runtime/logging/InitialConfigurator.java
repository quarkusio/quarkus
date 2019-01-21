package org.jboss.shamrock.runtime.logging;

import java.util.logging.Handler;
import java.util.logging.Level;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.DelayedHandler;

/**
 *
 */
public final class InitialConfigurator implements EmbeddedConfigurator {

    public static final DelayedHandler DELAYED_HANDLER = new DelayedHandler();

    public Level getMinimumLevelOf(final String loggerName) {
        return Level.ALL;
    }

    public Level getLevelOf(final String loggerName) {
        return loggerName.isEmpty() ? Level.ALL : null;
    }

    public Handler[] getHandlersOf(final String loggerName) {
        if (loggerName.isEmpty()) {
            if (ImageInfo.inImageBuildtimeCode()) {
                final ConsoleHandler handler = new ConsoleHandler(new PatternFormatter(
                    "%d{HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n"
                ));
                handler.setLevel(Level.INFO);
                handler.setFilter(new LogCleanupFilter());
                return new Handler[] {
                    handler
                };
            } else {
                return new Handler[] { DELAYED_HANDLER };
            }
        } else {
            return NO_HANDLERS;
        }
    }
}
