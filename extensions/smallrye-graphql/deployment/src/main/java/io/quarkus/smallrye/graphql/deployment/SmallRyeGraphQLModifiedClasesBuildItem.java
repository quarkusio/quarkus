package io.quarkus.smallrye.graphql.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

final class SmallRyeGraphQLModifiedClasesBuildItem extends SimpleBuildItem {

    private final Map<String, byte[]> modifiedClases;

    public SmallRyeGraphQLModifiedClasesBuildItem(Map<String, byte[]> modifiedClases) {
        this.modifiedClases = modifiedClases;
    }

    public Map<String, byte[]> getModifiedClases() {
        return modifiedClases;
    }
}
