package io.quarkus.mongo.deployment;

import com.mongodb.client.MongoClient;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class MongoClientBuildItem extends SimpleBuildItem {

    private final RuntimeValue<MongoClient> client;

    public MongoClientBuildItem(RuntimeValue<MongoClient> client) {
        this.client = client;
    }

    public RuntimeValue<MongoClient> getClient() {
        return client;
    }
}
