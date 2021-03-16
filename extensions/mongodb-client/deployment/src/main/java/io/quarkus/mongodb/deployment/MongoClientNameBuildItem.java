package io.quarkus.mongodb.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.mongodb.MongoClientName;

/**
 * Represents the values of the {@link MongoClientName}.
 */
public final class MongoClientNameBuildItem extends MultiBuildItem {

    private final String name;
    private final boolean addQualifier;

    public MongoClientNameBuildItem(String name) {
        this(name, true);
    }

    public MongoClientNameBuildItem(String name, boolean addQualifier) {
        this.name = name;
        this.addQualifier = addQualifier;
    }

    public String getName() {
        return name;
    }

    public boolean isAddQualifier() {
        return addQualifier;
    }
}
