package io.quarkus.mongodb.runtime;

import java.util.List;
import java.util.function.Function;

import com.mongodb.event.ConnectionPoolListener;

public class MongoClientSupport {
    private final List<String> bsonDiscriminators;
    private final List<Function<String, ConnectionPoolListener>> connectionPoolListenerFactories;
    private final List<ConnectionPoolListener> connectionPoolListeners;
    private final boolean disableSslSupport;

    public MongoClientSupport(
            List<String> bsonDiscriminators,
            List<Function<String, ConnectionPoolListener>> connectionPoolListenerFactories,
            List<ConnectionPoolListener> connectionPoolListeners,
            boolean disableSslSupport) {
        this.bsonDiscriminators = bsonDiscriminators;
        this.connectionPoolListenerFactories = connectionPoolListenerFactories;
        this.connectionPoolListeners = connectionPoolListeners;
        this.disableSslSupport = disableSslSupport;
    }

    public List<String> getBsonDiscriminators() {
        return bsonDiscriminators;
    }

    public List<Function<String, ConnectionPoolListener>> getConnectionPoolListenerFactories() {
        return connectionPoolListenerFactories;
    }

    @Deprecated(forRemoval = true, since = "3.33.0")
    public List<ConnectionPoolListener> getConnectionPoolListeners() {
        return connectionPoolListeners;
    }

    public boolean isDisableSslSupport() {
        return disableSslSupport;
    }
}
