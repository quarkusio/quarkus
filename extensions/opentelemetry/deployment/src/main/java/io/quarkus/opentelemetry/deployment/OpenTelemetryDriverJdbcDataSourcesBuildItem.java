package io.quarkus.opentelemetry.deployment;

import java.util.List;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains list of all {@link io.quarkus.agroal.spi.JdbcDataSourceBuildItem} using OpenTelemetryDriver.
 */
public final class OpenTelemetryDriverJdbcDataSourcesBuildItem extends SimpleBuildItem {

    public final List<JdbcDataSourceBuildItem> jdbcDataSources;

    OpenTelemetryDriverJdbcDataSourcesBuildItem(List<JdbcDataSourceBuildItem> jdbcDataSources) {
        this.jdbcDataSources = List.copyOf(jdbcDataSources);
    }
}
