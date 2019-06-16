package io.quarkus.mongo.deployment;

import com.mongodb.client.MongoClient;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.mongo.ReactiveMongoClient;
import io.quarkus.runtime.RuntimeValue;

public final class MongoClientBuildItem extends SimpleBuildItem {

    private final RuntimeValue<MongoClient> client;
    private final RuntimeValue<ReactiveMongoClient> reactive;

    public MongoClientBuildItem(RuntimeValue<MongoClient> client, RuntimeValue<ReactiveMongoClient> reactiveClient) {
        this.client = client;
        this.reactive = reactiveClient;
    }

    public RuntimeValue<MongoClient> getClient() {
        return client;
    }

    public RuntimeValue<ReactiveMongoClient> getReactive() {
        return reactive;
    }
}
