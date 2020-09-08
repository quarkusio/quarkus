package io.quarkus.mongodb.deployment;

import java.util.function.Supplier;

import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.builder.item.MultiBuildItem;

public final class MongoConnectionPoolListenerBuildItem extends MultiBuildItem {
    private Supplier<ConnectionPoolListener> connectionPoolListener;

    public MongoConnectionPoolListenerBuildItem(Supplier<ConnectionPoolListener> connectionPoolListener) {
        this.connectionPoolListener = connectionPoolListener;
    }

    public Supplier<ConnectionPoolListener> getConnectionPoolListener() {
        return connectionPoolListener;
    }
}
