package io.quarkus.mongodb.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class BsonDiscriminatorBuildItem extends SimpleBuildItem {

    private List<String> bsonDisciminatorClassNames;

    public BsonDiscriminatorBuildItem(List<String> bsonDisciminatorClassNames) {
        this.bsonDisciminatorClassNames = bsonDisciminatorClassNames;
    }

    public List<String> getBsonDisciminatorClassNames() {
        return bsonDisciminatorClassNames;
    }
}
