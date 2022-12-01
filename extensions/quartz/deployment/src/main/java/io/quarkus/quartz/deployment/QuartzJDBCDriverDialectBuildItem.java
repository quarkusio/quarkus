package io.quarkus.quartz.deployment;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the SQL driver dialect {@link org.quartz.impl.jdbcjobstore.StdJDBCDelegate driver delegate} to use.
 */
final class QuartzJDBCDriverDialectBuildItem extends SimpleBuildItem {
    private final Optional<String> driver;

    public QuartzJDBCDriverDialectBuildItem(Optional<String> driver) {
        this.driver = driver;
    }

    public Optional<String> getDriver() {
        return driver;
    }
}
