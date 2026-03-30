package io.quarkus.logging.json;

import java.util.Arrays;
import java.util.logging.Handler;

import org.jboss.logmanager.LogContext;

abstract class AbstractNamedJsonFormatterTest {

    protected static Handler getNamedHandler(String category, Class<?> handlerType) {
        return Arrays.stream(LogContext.getLogContext().getLogger(category).getHandlers())
                .filter(handlerType::isInstance)
                .findFirst().orElse(null);
    }
}
