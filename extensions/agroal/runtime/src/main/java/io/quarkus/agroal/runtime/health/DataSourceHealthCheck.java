package io.quarkus.agroal.runtime.health;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource.DataSourceLiteral;
import io.quarkus.arc.Arc;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesHealthSupport;

@Readiness
@ApplicationScoped
public class DataSourceHealthCheck implements HealthCheck {
    private final Map<String, DataSource> dataSources = new HashMap<>();

    @PostConstruct
    protected void init() {
        DataSourcesHealthSupport support = Arc.container().instance(DataSourcesHealthSupport.class)
                .get();
        Set<String> names = support.getConfiguredNames();
        Set<String> excludedNames = support.getExcludedNames();
        for (String name : names) {
            DataSource ds = DataSourceUtil.isDefault(name)
                    ? (DataSource) Arc.container().instance(DataSource.class).get()
                    : (DataSource) Arc.container().instance(DataSource.class, new DataSourceLiteral(name)).get();
            if (!excludedNames.contains(name) && ds != null) {
                dataSources.put(name, ds);
            }
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Database connections health check").up();
        for (Map.Entry<String, DataSource> dataSource : dataSources.entrySet()) {
            boolean isDefault = DataSourceUtil.isDefault(dataSource.getKey());
            AgroalDataSource ads = (AgroalDataSource) dataSource.getValue();
            try {
                boolean valid = ads.isHealthy(false);
                if (!valid) {
                    String data = isDefault ? "validation check failed for the default DataSource"
                            : "validation check failed for DataSource '" + dataSource.getKey() + "'";
                    String dsName = isDefault ? "default" : dataSource.getKey();
                    builder.down().withData(dsName, data);
                }
            } catch (SQLException e) {
                String data = isDefault ? "Unable to execute the validation check for the default DataSource: "
                        : "Unable to execute the validation check for DataSource '" + dataSource.getKey() + "': ";
                String dsName = isDefault ? "default" : dataSource.getKey();
                builder.down().withData(dsName, data + e.getMessage());
            }
        }
        return builder.build();
    }
}
