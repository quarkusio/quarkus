package io.quarkus.logging;

import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;

import io.quarkus.bootstrap.graal.ImageInfo;
import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.runtime.logging.LoggingSetupRecorder;

public final class InitialConfigurator implements EmbeddedConfigurator {

    public InitialConfigurator() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                if (!QuarkusDelayedHandler.INSTANCE.isActivated()) {
                    LoggingSetupRecorder.handleFailedStart();
                }
            }
        }));
    }

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
                return new Handler[] { QuarkusDelayedHandler.INSTANCE };
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
