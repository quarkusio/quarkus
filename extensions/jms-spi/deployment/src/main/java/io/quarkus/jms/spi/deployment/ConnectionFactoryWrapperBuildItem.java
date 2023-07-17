package io.quarkus.jms.spi.deployment;

import java.util.function.Function;

import jakarta.jms.ConnectionFactory;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that can be used to wrap the JMS ConnectionFactory
 */
public final class ConnectionFactoryWrapperBuildItem extends SimpleBuildItem {
    private final Function<ConnectionFactory, Object> wrapper;

    public ConnectionFactoryWrapperBuildItem(Function<ConnectionFactory, Object> wrapper) {
        if (wrapper == null) {
            throw new AssertionError("wrapper is required");
        }
        this.wrapper = wrapper;
    }

    public Function<ConnectionFactory, Object> getWrapper() {
        return wrapper;
    }
}
