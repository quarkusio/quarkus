package io.quarkus.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.handlers.DelayedHandler;
import org.junit.jupiter.api.Assertions;

import io.quarkus.bootstrap.logging.InitialConfigurator;

public class LoggingTestsHelper {

    public static Handler getHandler(Class clazz) {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Optional<Handler> handler = Arrays.stream(delayedHandler.getHandlers()).filter(h -> (clazz.isInstance(h)))
                .findFirst();
        Assertions.assertTrue(handler.isPresent(), () -> String.format("Could not find handler of type %s: %s", clazz,
                Arrays.asList(delayedHandler.getHandlers())));
        return handler.get();
    }
}
