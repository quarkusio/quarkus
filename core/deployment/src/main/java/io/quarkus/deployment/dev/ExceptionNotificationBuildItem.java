package io.quarkus.deployment.dev;

import java.util.function.BiConsumer;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows for a handler to be registered when exceptions are logged.
 *
 * This is intended for use in dev/test mode to allow Quarkus to help the developer handle the issue.
 */
public final class ExceptionNotificationBuildItem extends MultiBuildItem {

    final BiConsumer<Throwable, StackTraceElement> exceptionHandler;

    public ExceptionNotificationBuildItem(BiConsumer<Throwable, StackTraceElement> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public BiConsumer<Throwable, StackTraceElement> getExceptionHandler() {
        return exceptionHandler;
    }
}
