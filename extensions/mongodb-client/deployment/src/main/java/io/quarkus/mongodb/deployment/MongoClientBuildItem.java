package io.quarkus.mongodb.deployment;

import com.mongodb.client.MongoClient;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.mongodb.ReactiveMongoClient;
import io.quarkus.runtime.RuntimeValue;

public final class MongoClientBuildItem extends MultiBuildItem {
    private final RuntimeValue<MongoClient> client;
    private final RuntimeValue<ReactiveMongoClient> reactive;
    private final String name;

    public MongoClientBuildItem(RuntimeValue<MongoClient> client, RuntimeValue<ReactiveMongoClient> reactiveClient,
            String name) {
        this.client = client;
        this.reactive = reactiveClient;
        this.name = name;
    }

    public RuntimeValue<MongoClient> getClient() {
        return client;
    }

    public RuntimeValue<ReactiveMongoClient> getReactive() {
        return reactive;
    }

    public String getName() {
        return name;
    }
}
