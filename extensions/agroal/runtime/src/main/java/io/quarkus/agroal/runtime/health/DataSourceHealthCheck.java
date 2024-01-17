package io.quarkus.agroal.runtime.health;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceSupport;

@Readiness
@ApplicationScoped
public class DataSourceHealthCheck implements HealthCheck {

    @Inject
    Instance<DataSources> dataSources;

    private final Map<String, DataSource> checkedDataSources = new HashMap<>();

    @PostConstruct
    protected void init() {
        if (!dataSources.isResolvable()) {
            // No configured Agroal datasource at build time.
            return;
        }
        DataSourceSupport support = Arc.container().instance(DataSourceSupport.class)
                .get();
        Set<String> names = support.getConfiguredNames();
        Set<String> excludedNames = support.getInactiveOrHealthCheckExcludedNames();
        for (String name : names) {
            if (excludedNames.contains(name)) {
                continue;
            }
            DataSource ds = dataSources.get().getDataSource(name);
            if (ds != null) {
                checkedDataSources.put(name, ds);
            }
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Database connections health check").up();
        for (Map.Entry<String, DataSource> dataSource : checkedDataSources.entrySet()) {
            boolean isDefault = DataSourceUtil.isDefault(dataSource.getKey());
            AgroalDataSource ads = (AgroalDataSource) dataSource.getValue();
            String dsName = dataSource.getKey();

            try {
                boolean valid = ads.isHealthy(false);
                if (!valid) {
                    String data = isDefault ? "validation check failed for the default DataSource"
                            : "validation check failed for DataSource '" + dataSource.getKey() + "'";
                    builder.down().withData(dsName, data);
                } else {
                    builder.withData(dsName, "UP");
                }
            } catch (SQLException e) {
                String data = isDefault ? "Unable to execute the validation check for the default DataSource: "
                        : "Unable to execute the validation check for DataSource '" + dataSource.getKey() + "': ";
                builder.down().withData(dsName, data + e.getMessage());
            }
        }
        return builder.build();
    }
}
