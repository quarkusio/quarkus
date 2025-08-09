package io.quarkus.deployment.logging;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Signal indicating that a log stream should be enabled.
 * Should ensure a log stream handler is active, often used for dev mode
 */
public final class LogStreamBuildItem extends MultiBuildItem {
}
