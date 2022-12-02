package io.quarkus.mongodb.runtime;

import java.util.List;

import com.mongodb.event.ConnectionPoolListener;

public class MongoClientSupport {

    private final List<String> bsonDiscriminators;
    private final List<ConnectionPoolListener> connectionPoolListeners;
    private final boolean disableSslSupport;

    public MongoClientSupport(List<String> bsonDiscriminators,
            List<ConnectionPoolListener> connectionPoolListeners, boolean disableSslSupport) {
        this.bsonDiscriminators = bsonDiscriminators;
        this.connectionPoolListeners = connectionPoolListeners;
        this.disableSslSupport = disableSslSupport;
    }

    public List<String> getBsonDiscriminators() {
        return bsonDiscriminators;
    }

    public List<ConnectionPoolListener> getConnectionPoolListeners() {
        return connectionPoolListeners;
    }

    public boolean isDisableSslSupport() {
        return disableSslSupport;
    }
}
