package io.quarkus.smallrye.graphql.client.deployment;

import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.SimpleBuildItem;

final class SmallRyeGraphQLClientFinalIndexBuildItem extends SimpleBuildItem {

    private final IndexView index;

    public SmallRyeGraphQLClientFinalIndexBuildItem(IndexView index) {
        this.index = index;
    }

    public IndexView getFinalIndex() {
        return index;
    }
}
