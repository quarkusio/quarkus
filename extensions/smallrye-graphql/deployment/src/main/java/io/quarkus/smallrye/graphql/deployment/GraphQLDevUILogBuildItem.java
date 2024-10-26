package io.quarkus.smallrye.graphql.deployment;

import java.util.concurrent.SubmissionPublisher;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Used to create the publisher for the graphql trafic log in dev ui
 */
public final class GraphQLDevUILogBuildItem extends SimpleBuildItem {
    private final RuntimeValue<SubmissionPublisher<String>> publisher;

    public GraphQLDevUILogBuildItem(RuntimeValue<SubmissionPublisher<String>> publisher) {
        this.publisher = publisher;
    }

    public RuntimeValue<SubmissionPublisher<String>> getPublisher() {
        return this.publisher;
    }
}
