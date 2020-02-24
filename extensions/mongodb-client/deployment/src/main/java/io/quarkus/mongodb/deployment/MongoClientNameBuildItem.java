package io.quarkus.mongodb.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.mongodb.runtime.MongoClientName;

/**
 * Represents the values of the {@link MongoClientName}
 */
final class MongoClientNameBuildItem extends MultiBuildItem {

    private final String name;

    public MongoClientNameBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
