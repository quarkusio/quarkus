package io.quarkus.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.handlers.DelayedHandler;

import io.quarkus.runtime.logging.InitialConfigurator;

public class LoggingTestsHelper {

    public static Handler getHandler(Class clazz) {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Handler handler = Arrays.stream(delayedHandler.getHandlers()).filter(h -> (clazz.isInstance(h)))
                .findFirst().get();
        assertThat(handler).isNotNull();
        return handler;
    }
}
