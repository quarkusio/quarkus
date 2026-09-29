package io.quarkus.datasource.runtime.produi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;

/**
 * Read-only view of the configured datasources, shared by Dev UI and Prod UI.
 * It exposes only non-sensitive configuration - name, database kind/version,
 * active flag and health-check exclusion. It never exposes the JDBC URL,
 * username, password or credentials provider, so it is safe to serve in
 * production. Returns plain records so no JSON library is needed on the runtime
 * classpath.
 */
@ApplicationScoped
public class DataSourceProdUIService {

    @Inject
    DataSourcesBuildTimeConfig buildTimeConfig;

    @Inject
    DataSourcesRuntimeConfig runtimeConfig;

    @NonBlocking
    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("List all configured datasources and their non-sensitive configuration")
    public List<DatasourceInfo> getDatasources() {
        // Sort by name for a stable display; TreeMap keeps the default first.
        Map<String, DataSourceBuildTimeConfig> sorted = new TreeMap<>(buildTimeConfig.dataSources());

        List<DatasourceInfo> result = new ArrayList<>();
        for (Map.Entry<String, DataSourceBuildTimeConfig> entry : sorted.entrySet()) {
            String name = entry.getKey();
            DataSourceBuildTimeConfig bt = entry.getValue();

            DataSourceRuntimeConfig rt = runtimeConfig.dataSources().get(name);
            String active = (rt != null && rt.active().isPresent())
                    ? String.valueOf(rt.active().get())
                    : "auto";

            result.add(new DatasourceInfo(
                    DataSourceUtil.isDefault(name) ? "<default>" : name,
                    bt.dbKind().orElse("unknown"),
                    bt.dbVersion().orElse("-"),
                    active,
                    bt.healthExclude()));
        }
        return result;
    }

    public record DatasourceInfo(String name, String dbKind, String dbVersion, String active, boolean healthExcluded) {
    }
}
