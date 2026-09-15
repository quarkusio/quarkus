package io.quarkus.agroal.runtime.produi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;

/**
 * Read-only view of the Agroal JDBC connection pools, shared by Dev UI and
 * Prod UI. Only pool metrics and pool sizing are exposed - no SQL execution,
 * schema browsing or connection details - so it is safe to expose in
 * production. Metric counters read as zero unless datasource metrics are
 * enabled ({@code quarkus.datasource.jdbc.enable-metrics=true}).
 */
@ApplicationScoped
public class AgroalPoolProdUIService {

    @NonBlocking
    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("Get JDBC connection pool metrics for all active datasources")
    public List<PoolInfo> getPools() {
        List<PoolInfo> pools = new ArrayList<>();
        for (String name : new TreeSet<>(AgroalDataSourceUtil.activeDataSourceNames())) {
            Optional<AgroalDataSource> dataSource = AgroalDataSourceUtil.dataSourceIfActive(name);
            if (dataSource.isEmpty()) {
                continue;
            }
            pools.add(toPoolInfo(name, dataSource.get()));
        }
        return pools;
    }

    private PoolInfo toPoolInfo(String name, AgroalDataSource dataSource) {
        AgroalDataSourceMetrics m = dataSource.getMetrics();
        AgroalConnectionPoolConfiguration pool = dataSource.getConfiguration().connectionPoolConfiguration();
        return new PoolInfo(
                DataSourceUtil.isDefault(name) ? "<default>" : name,
                m.activeCount(),
                m.availableCount(),
                m.maxUsedCount(),
                m.awaitingCount(),
                pool.maxSize(),
                pool.minSize(),
                m.acquireCount(),
                m.creationCount(),
                m.leakDetectionCount(),
                m.invalidCount(),
                m.flushCount(),
                m.reapCount(),
                m.destroyCount(),
                m.blockingTimeAverage().toMillis(),
                m.blockingTimeMax().toMillis(),
                m.creationTimeAverage().toMillis(),
                m.creationTimeMax().toMillis());
    }

    public record PoolInfo(
            String name,
            long active,
            long available,
            long maxUsed,
            long awaiting,
            int maxSize,
            int minSize,
            long acquireCount,
            long creationCount,
            long leakDetectionCount,
            long invalidCount,
            long flushCount,
            long reapCount,
            long destroyCount,
            long blockingTimeAverageMs,
            long blockingTimeMaxMs,
            long creationTimeAverageMs,
            long creationTimeMaxMs) {
    }
}
