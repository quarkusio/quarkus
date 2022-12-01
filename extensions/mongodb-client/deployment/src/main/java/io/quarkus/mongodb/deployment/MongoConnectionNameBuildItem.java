package io.quarkus.mongodb.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Holds a MongoDB connection name.
 */
final class MongoConnectionNameBuildItem extends MultiBuildItem {

    private final String name;

    public MongoConnectionNameBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
