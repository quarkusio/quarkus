package io.quarkus.quartz.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.quartz.runtime.jdbc.JDBCDataSource;

/**
 * Holds the necessary information to determine SQL driver dialect.
 * <p>
 * This can mean either a custom driver registered by user or one of Quarkus built-in drivers.
 * If it is the latter, we defer discovering the driver to runtime, see also {@link io.quarkus.quartz.runtime.QuartzSupport}.
 */
final class QuartzJDBCDriverDialectBuildItem extends SimpleBuildItem {
    private final Optional<String> driver;
    private final List<JDBCDataSource> dataSources;

    /**
     * Driver represented as a {@code String}, can be empty even if configured during build time in case there is no DB.
     * <p>
     * The list of data sources are only used if the driver needs to be determined during runtime.
     * The list can be null if the driver has been determined during build time.
     *
     * @param driver driver to be used, optionally empty
     * @param dataSources can be null which indicates that the driver has been resolved during build time
     */
    public QuartzJDBCDriverDialectBuildItem(Optional<String> driver, List<JDBCDataSource> dataSources) {
        this.driver = driver;
        this.dataSources = dataSources;
    }

    public Optional<String> getDriver() {
        return driver;
    }

    public List<JDBCDataSource> getDataSources() {
        return dataSources;
    }
}
