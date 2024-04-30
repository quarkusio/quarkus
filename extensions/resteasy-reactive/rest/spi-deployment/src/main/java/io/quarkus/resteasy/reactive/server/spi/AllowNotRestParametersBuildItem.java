package io.quarkus.resteasy.reactive.server.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item which extensions can generate when they want to allow RESTEasy Reactive methods
 * with parameters that are annotated with not REST annotations. This allows RESTEasy Reactive to let mixed parameters coexist
 * within resource methods signature
 */
public final class AllowNotRestParametersBuildItem extends SimpleBuildItem {
}
