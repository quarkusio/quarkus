package io.quarkus.reactive.oracle.client.deployment;

import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.vertx.oracleclient.OraclePool;

@Deprecated(since = "3.21", forRemoval = true)
public final class OraclePoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;

    private final Function<SyntheticCreationalContext<OraclePool>, OraclePool> oraclePool;

    public OraclePoolBuildItem(String dataSourceName, Function<SyntheticCreationalContext<OraclePool>, OraclePool> oraclePool) {
        this.dataSourceName = dataSourceName;
        this.oraclePool = oraclePool;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public Function<SyntheticCreationalContext<OraclePool>, OraclePool> getOraclePool() {
        return oraclePool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }

}
