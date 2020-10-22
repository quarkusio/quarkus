package io.quarkus.mongodb.runtime;

import java.util.List;

import com.mongodb.event.ConnectionPoolListener;

public class MongoClientSupport {

    private final List<String> codecProviders;
    private final List<String> propertyCodecProviders;
    private final List<String> bsonDiscriminators;
    private final List<ConnectionPoolListener> connectionPoolListeners;
    private final List<String> commandListeners;
    private final boolean disableSslSupport;

    public MongoClientSupport(List<String> codecProviders, List<String> propertyCodecProviders, List<String> bsonDiscriminators,
            List<String> commandListeners, List<ConnectionPoolListener> connectionPoolListeners, boolean disableSslSupport) {
        this.codecProviders = codecProviders;
        this.propertyCodecProviders = propertyCodecProviders;
        this.bsonDiscriminators = bsonDiscriminators;
        this.connectionPoolListeners = connectionPoolListeners;
        this.commandListeners = commandListeners;
        this.disableSslSupport = disableSslSupport;
    }

    public List<String> getCodecProviders() {
        return codecProviders;
    }

    public List<String> getPropertyCodecProviders() {
        return propertyCodecProviders;
    }

    public List<String> getBsonDiscriminators() {
        return bsonDiscriminators;
    }

    public List<ConnectionPoolListener> getConnectionPoolListeners() {
        return connectionPoolListeners;
    }

    public List<String> getCommandListeners() {
        return commandListeners;
    }

    public boolean isDisableSslSupport() {
        return disableSslSupport;
    }
}
