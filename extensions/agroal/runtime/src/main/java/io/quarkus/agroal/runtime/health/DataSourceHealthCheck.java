package io.quarkus.agroal.runtime.health;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.AgroalDataSourceSupport;
import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceSupport;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;

@Readiness
@ApplicationScoped
public class DataSourceHealthCheck implements HealthCheck {

    @Inject
    Instance<DataSourceSupport> dataSourceSupport;

    @Inject
    Instance<AgroalDataSourceSupport> agroalDataSourceSupport;

    @Inject
    DataSourcesRuntimeConfig dataSourcesRuntimeConfig;

    private final Map<String, CheckedDataSource> checkedDataSources = new HashMap<>();

    @PostConstruct
    protected void init() {
        if (!dataSourceSupport.isResolvable() || !agroalDataSourceSupport.isResolvable()) {
            // No configured Agroal datasources at build time.
            return;
        }
        DataSourceSupport support = dataSourceSupport.get();
        Set<String> healthCheckExcludedNames = support.getHealthCheckExcludedNames();
        for (String name : agroalDataSourceSupport.get().entries.keySet()) {
            if (healthCheckExcludedNames.contains(name)) {
                continue;
            }
            Optional<AgroalDataSource> dataSource = AgroalDataSourceUtil.dataSourceIfActive(name);
            if (dataSource.isPresent()) {
                long ttlNanos = dataSourcesRuntimeConfig.dataSources().get(name).health().ttl().toNanos();
                checkedDataSources.put(name, new CheckedDataSource(dataSource.get(), ttlNanos));
            }
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Database connections health check").up();
        for (Map.Entry<String, CheckedDataSource> entry : checkedDataSources.entrySet()) {
            String dsName = entry.getKey();
            CheckedDataSource cds = entry.getValue();
            boolean isDefault = DataSourceUtil.isDefault(dsName);

            CachedResult cached = cds.cachedResult;
            if (cached != null && cds.ttlNanos > 0 && (System.nanoTime() - cached.checkedAt()) < cds.ttlNanos) {
                if (cached.healthy()) {
                    builder.withData(dsName, "UP");
                } else {
                    builder.down().withData(dsName, cached.detail());
                }
                continue;
            }

            try {
                boolean valid = cds.dataSource.isHealthy(false);
                if (!valid) {
                    String data = isDefault ? "validation check failed for the default DataSource"
                            : "validation check failed for DataSource '" + dsName + "'";
                    builder.down().withData(dsName, data);
                    if (cds.ttlNanos > 0) {
                        cds.cachedResult = new CachedResult(false, data, System.nanoTime());
                    }
                } else {
                    builder.withData(dsName, "UP");
                    if (cds.ttlNanos > 0) {
                        cds.cachedResult = new CachedResult(true, null, System.nanoTime());
                    }
                }
            } catch (SQLException e) {
                String data = isDefault ? "Unable to execute the validation check for the default DataSource: "
                        : "Unable to execute the validation check for DataSource '" + dsName + "': ";
                builder.down().withData(dsName, data + e.getMessage());
                if (cds.ttlNanos > 0) {
                    cds.cachedResult = new CachedResult(false, data + e.getMessage(), System.nanoTime());
                }
            }
        }
        return builder.build();
    }

    protected Map<String, DataSource> getCheckedDataSources() {
        Map<String, DataSource> result = new HashMap<>();
        for (Map.Entry<String, CheckedDataSource> entry : checkedDataSources.entrySet()) {
            result.put(entry.getKey(), entry.getValue().dataSource);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Immutable snapshot of a single datasource health check result.
     * Swapped atomically via a volatile reference in {@link CheckedDataSource}.
     */
    record CachedResult(boolean healthy, String detail, long checkedAt) {
    }

    /**
     * Groups a datasource with its per-datasource health check TTL and the most recent cached result.
     */
    static final class CheckedDataSource {
        final AgroalDataSource dataSource;
        final long ttlNanos;
        volatile CachedResult cachedResult;

        CheckedDataSource(AgroalDataSource dataSource, long ttlNanos) {
            this.dataSource = dataSource;
            this.ttlNanos = ttlNanos;
        }
    }
}
