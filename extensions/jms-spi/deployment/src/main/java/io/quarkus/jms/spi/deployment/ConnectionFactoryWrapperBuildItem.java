package io.quarkus.jms.spi.deployment;

import java.util.function.Function;

import jakarta.jms.ConnectionFactory;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that can be used to wrap the JMS ConnectionFactory
 *
 * @deprecated This build item is deprecated and will be removed.
 *             Use the SPI module from Quarkiverse `quarkus-jms` extension instead
 */
@Deprecated(forRemoval = true, since = "4.0")
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
