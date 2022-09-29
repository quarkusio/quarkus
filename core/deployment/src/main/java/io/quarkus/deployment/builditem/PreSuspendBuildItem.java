package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.deployment.annotations.Produce;

/**
 * A build item that pre-suspend steps must produce.
 * Use with {@link Produce}.
 */
public final class PreSuspendBuildItem extends EmptyBuildItem {
}
