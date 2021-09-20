package io.quarkus.agroal.runtime.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.sql.DataSource;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesExcludedFromHealthChecks;

@Readiness
@ApplicationScoped
public class DataSourceHealthCheck implements HealthCheck {
    private static final String DEFAULT_DS = "__default__";
    private final Map<String, DataSource> dataSources = new HashMap<>();

    @PostConstruct
    protected void init() {
        Set<Bean<?>> beans = Arc.container().beanManager().getBeans(DataSource.class);
        DataSourcesExcludedFromHealthChecks excluded = Arc.container().instance(DataSourcesExcludedFromHealthChecks.class)
                .get();
        Set<String> excludedNames = excluded.getExcludedNames();
        for (Bean<?> bean : beans) {
            if (bean.getName() == null) {
                if (!excludedNames.contains(DataSourceUtil.DEFAULT_DATASOURCE_NAME)) {
                    // this is the default DataSource: retrieve it by type
                    DataSource defaultDs = Arc.container().instance(DataSource.class).get();
                    dataSources.put(DEFAULT_DS, defaultDs);
                }
            } else {
                if (!excludedNames.contains(bean.getName())) {
                    DataSource ds = (DataSource) Arc.container().instance(bean.getName()).get();
                    dataSources.put(bean.getName(), ds);
                }
            }
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Database connections health check").up();
        for (Map.Entry<String, DataSource> dataSource : dataSources.entrySet()) {
            boolean isDefault = DEFAULT_DS.equals(dataSource.getKey());
            try (Connection con = dataSource.getValue().getConnection()) {
                boolean valid = con.isValid(0);
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
