package io.quarkus.vertx.core.runtime;

import io.vertx.core.spi.logging.LogDelegate;
import io.vertx.core.spi.logging.LogDelegateFactory;

public class VertxLogDelegateFactory implements LogDelegateFactory {
    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LogDelegate createDelegate(String name) {
        return new VertxLogDelegate(name);
    }
}
