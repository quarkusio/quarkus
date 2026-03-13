package io.quarkus.mongodb.deployment.spi;

import java.util.function.Function;

import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Registers a new {@link com.mongodb.event.ConnectionPoolListener}.
 */
public final class MongoConnectionPoolListenerBuildItem extends MultiBuildItem {
    private final Function<String, ConnectionPoolListener> connectionPoolListener;

    MongoConnectionPoolListenerBuildItem(Function<String, ConnectionPoolListener> connectionPoolListener) {
        this.connectionPoolListener = connectionPoolListener;
    }

    public Function<String, ConnectionPoolListener> getConnectionPoolListener() {
        return connectionPoolListener;
    }

    /**
     * Registers a new {@link ConnectionPoolListener}. The listener is the result of a function call,
     * with the input being a {@code String} containing the MongoDB client name to which the
     * {@link ConnectionPoolListener} is being registered.
     *
     * @param connectionPoolListener a {@code Function} of {@code String} with the MongoDB client name and
     *        {@code ConnectionPoolListener} to register.
     * @return the {@link MongoConnectionPoolListenerBuildItem}
     */
    public static MongoConnectionPoolListenerBuildItem of(Function<String, ConnectionPoolListener> connectionPoolListener) {
        return new MongoConnectionPoolListenerBuildItem(connectionPoolListener);
    }
}
