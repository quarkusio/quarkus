package io.quarkus.mongodb.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class BsonDiscriminatorBuildItem extends SimpleBuildItem {

    private List<String> bsonDiscriminatorClassNames;

    public BsonDiscriminatorBuildItem(List<String> bsonDiscriminatorClassNames) {
        this.bsonDiscriminatorClassNames = bsonDiscriminatorClassNames;
    }

    public List<String> getBsonDiscriminatorClassNames() {
        return bsonDiscriminatorClassNames;
    }
}
