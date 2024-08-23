package io.quarkus.smallrye.graphql.deployment;

import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.SimpleBuildItem;

final class SmallRyeGraphQLFinalIndexBuildItem extends SimpleBuildItem {

    private final IndexView index;

    public SmallRyeGraphQLFinalIndexBuildItem(IndexView index) {
        this.index = index;
    }

    public IndexView getFinalIndex() {
        return index;
    }
}
