package io.quarkus.datasource.runtime;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class that holds the names of all configured data sources,
 * along with the names of those that are inactive or excluded from health checks.
 * <p>
 * This is used by any feature that needs runtime access to data sources,
 * e.g. Flyway/Liquibase or health check implementation classes.
 */
public class DataSourceSupport {

    private final Set<String> healthCheckExcludedNames;
    private final Set<String> inactiveNames;
    private final Set<String> inactiveOrHealthCheckExcludedNames;

    public DataSourceSupport(Set<String> healthCheckExcludedNames,
            Set<String> inactiveNames) {
        this.healthCheckExcludedNames = healthCheckExcludedNames;
        this.inactiveOrHealthCheckExcludedNames = new HashSet<>();
        inactiveOrHealthCheckExcludedNames.addAll(inactiveNames);
        inactiveOrHealthCheckExcludedNames.addAll(healthCheckExcludedNames);
        this.inactiveNames = inactiveNames;
    }

    /**
     * @deprecated This may not account for datasources deactivated automatically (due to missing configuration, ...).
     *             To check if a datasource bean is active, use
     *             {@code Arc.container().select(...).getHandle().getBean().isActive()}.
     *             Alternatively, to check if a datasource is active, use the utils
     *             {@code AgroalDataSourceUtil#dataSourceIfActive(...)}/{@code AgroalDataSourceUtil#activeDataSourceNames()}
     *             or
     *             {@code ReactiveDataSourceUtil#dataSourceIfActive(...)}/{@code ReactiveDataSourceUtil#activeDataSourceNames()}.
     */
    @Deprecated
    public Set<String> getInactiveNames() {
        return inactiveNames;
    }

    /**
     * @deprecated This may not account for datasources deactivated automatically (due to missing configuration, ...).
     *             To check if a datasource is excluded from health checks, use {@link #getHealthCheckExcludedNames()}.
     *             To check if a datasource bean is active, use
     *             {@code Arc.container().select(...).getHandle().getBean().isActive()}.
     *             Alternatively, to check if a datasource is active, use the utils
     *             {@code AgroalDataSourceUtil#dataSourceIfActive(...)}/{@code AgroalDataSourceUtil#activeDataSourceNames()}
     *             or
     *             {@code ReactiveDataSourceUtil#dataSourceIfActive(...)}/{@code ReactiveDataSourceUtil#activeDataSourceNames()}.
     */
    @Deprecated
    public Set<String> getInactiveOrHealthCheckExcludedNames() {
        return inactiveOrHealthCheckExcludedNames;
    }

    public Set<String> getHealthCheckExcludedNames() {
        return healthCheckExcludedNames;
    }
}
