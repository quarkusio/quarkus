package io.quarkus.mongodb.deployment.spi;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The unique list of MongoDB client names.
 */
public final class MongoClientsBuildItem extends SimpleBuildItem {
    private final List<MongoClientBuildItem> mongoClients;

    MongoClientsBuildItem(List<MongoClientBuildItem> mongoClients) {
        this.mongoClients = mongoClients;
    }

    public List<MongoClientBuildItem> getMongoClients() {
        return mongoClients;
    }

    public static MongoClientsBuildItem of(List<MongoClientBuildItem> mongoClients) {
        return new MongoClientsBuildItem(mongoClients);
    }
}
