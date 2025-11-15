package io.quarkus.quartz.runtime;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.quartz.Nonconcurrent;
import io.quarkus.quartz.runtime.jdbc.JDBCDataSource;
import io.quarkus.quartz.runtime.jdbc.QuarkusDBv8Delegate;
import io.quarkus.quartz.runtime.jdbc.QuarkusHSQLDBDelegate;
import io.quarkus.quartz.runtime.jdbc.QuarkusMSSQLDelegate;
import io.quarkus.quartz.runtime.jdbc.QuarkusPostgreSQLDelegate;
import io.quarkus.quartz.runtime.jdbc.QuarkusStdJDBCDelegate;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;

public class QuartzSupport {

    private final QuartzRuntimeConfig runtimeConfig;
    private final QuartzBuildTimeConfig buildTimeConfig;
    private final Optional<String> driverDialect;
    // <FQCN>#<method_name>
    private final Set<String> nonconcurrentMethods;

    public QuartzSupport(QuartzRuntimeConfig runtimeConfig, QuartzBuildTimeConfig buildTimeConfig,
            Optional<String> driverDialect, List<JDBCDataSource> dataSources, Set<String> nonconcurrentMethods) {
        this.runtimeConfig = runtimeConfig;
        this.buildTimeConfig = buildTimeConfig;
        this.nonconcurrentMethods = Set.copyOf(nonconcurrentMethods);

        if (dataSources == null) {
            this.driverDialect = driverDialect;
        } else {
            if (runtimeConfig.deferredDatasourceName().isEmpty()) {
                throw new ConfigurationException(
                        "Deferred datasource name is missing - you can configure it via quarkus.quartz.deferred-datasource-name.");
            }
            // Determine the driver dialect
            Optional<JDBCDataSource> selectedDataSource = dataSources.stream()
                    .filter(i -> runtimeConfig.deferredDatasourceName().get().equals(i.getName()))
                    .findFirst();

            if (!selectedDataSource.isPresent()) {
                String message = String.format(
                        "JDBC Store configured but the '%s' datasource is not configured properly. You can configure your datasource by following the guide available at: https://quarkus.io/guides/datasource",
                        runtimeConfig.deferredDatasourceName().isPresent() ? runtimeConfig.deferredDatasourceName().get()
                                : "default");
                throw new ConfigurationException(message);
            }

            String dataSourceKind = selectedDataSource.get().getDbKind();
            if (DatabaseKind.isPostgreSQL(dataSourceKind)) {
                this.driverDialect = Optional.of(QuarkusPostgreSQLDelegate.class.getName());
            } else if (DatabaseKind.isH2(dataSourceKind)) {
                this.driverDialect = Optional.of(QuarkusHSQLDBDelegate.class.getName());
            } else if (DatabaseKind.isMsSQL(dataSourceKind)) {
                this.driverDialect = Optional.of(QuarkusMSSQLDelegate.class.getName());
            } else if (DatabaseKind.isDB2(dataSourceKind)) {
                this.driverDialect = Optional.of(QuarkusDBv8Delegate.class.getName());
            } else {
                this.driverDialect = Optional.of(QuarkusStdJDBCDelegate.class.getName());
            }
        }
    }

    public QuartzRuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }

    public QuartzBuildTimeConfig getBuildTimeConfig() {
        return buildTimeConfig;
    }

    public Optional<String> getDriverDialect() {
        return driverDialect;
    }

    /**
     *
     * @param method
     * @return {@code true} if the scheduled method is annotated with {@link Nonconcurrent}
     */
    public boolean isNonconcurrent(ScheduledMethod method) {
        return nonconcurrentMethods.contains(method.getMethodDescription());
    }

}
