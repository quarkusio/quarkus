package io.quarkus.vertx.http.deployment;

import java.util.concurrent.SubmissionPublisher;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Used to create the publisher for the vertx access log in dev ui
 */
public final class VertxDevUILogBuildItem extends SimpleBuildItem {

    private final RuntimeValue<SubmissionPublisher<String>> publisher;

    public VertxDevUILogBuildItem(RuntimeValue<SubmissionPublisher<String>> publisher) {
        this.publisher = publisher;
    }

    public RuntimeValue<SubmissionPublisher<String>> getPublisher() {
        return this.publisher;
    }
}
