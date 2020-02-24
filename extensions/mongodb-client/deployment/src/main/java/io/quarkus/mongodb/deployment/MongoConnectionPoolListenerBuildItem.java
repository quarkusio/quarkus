package io.quarkus.mongodb.deployment;

import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.builder.item.MultiBuildItem;

public final class MongoConnectionPoolListenerBuildItem extends MultiBuildItem {
    private ConnectionPoolListener connectionPoolListener;

    public MongoConnectionPoolListenerBuildItem(ConnectionPoolListener connectionPoolListener) {
        this.connectionPoolListener = connectionPoolListener;
    }

    public ConnectionPoolListener getConnectionPoolListener() {
        return connectionPoolListener;
    }
}
