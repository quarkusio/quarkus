package io.quarkus.smallrye.graphql.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

final class SmallRyeGraphQLIndexBuildItem extends SimpleBuildItem {

    private final Map<String, byte[]> modifiedClases;

    public SmallRyeGraphQLIndexBuildItem(Map<String, byte[]> modifiedClases) {
        this.modifiedClases = modifiedClases;
    }

    public Map<String, byte[]> getModifiedClases() {
        return modifiedClases;
    }
}
