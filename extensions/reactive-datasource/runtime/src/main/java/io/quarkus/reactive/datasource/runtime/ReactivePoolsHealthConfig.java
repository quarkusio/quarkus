package io.quarkus.reactive.datasource.runtime;

import java.util.Map;

/**
 * Runtime configuration for reactive pool health checks.
 * Maps datasource names to the SQL query used for health checking.
 */
public class ReactivePoolsHealthConfig {

    private final Map<String, String> healthCheckSqlByDatasource;

    public ReactivePoolsHealthConfig(Map<String, String> healthCheckSqlByDatasource) {
        this.healthCheckSqlByDatasource = Map.copyOf(healthCheckSqlByDatasource);
    }

    public Map<String, String> getHealthCheckSqlByDatasource() {
        return healthCheckSqlByDatasource;
    }
}
