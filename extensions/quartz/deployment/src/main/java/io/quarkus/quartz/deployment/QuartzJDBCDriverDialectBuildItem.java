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
    private List<JDBCDataSource> dataSources;

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
