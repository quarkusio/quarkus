package io.quarkus.agroal.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Purely marker build item that tells us to prepare OpenTelemetry JDBC instrumentation.
 */
final class OpenTelemetryJDBCInstrumentationBuildItem extends SimpleBuildItem {
}
