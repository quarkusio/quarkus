package io.quarkus.smallrye.graphql.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

final class SmallRyeGraphQLBuildItem extends SimpleBuildItem {

    private final String graphqlUiFinalDestination;
    private final String graphqlUiPath;

    public SmallRyeGraphQLBuildItem(String graphqlUiFinalDestination, String graphqlUiPath) {
        this.graphqlUiFinalDestination = graphqlUiFinalDestination;
        this.graphqlUiPath = graphqlUiPath;
    }

    public String getGraphqlUiFinalDestination() {
        return graphqlUiFinalDestination;
    }

    public String getGraphqlUiPath() {
        return graphqlUiPath;
    }
}