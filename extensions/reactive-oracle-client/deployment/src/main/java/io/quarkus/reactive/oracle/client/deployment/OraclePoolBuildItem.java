package io.quarkus.reactive.oracle.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.oracleclient.OraclePool;

public final class OraclePoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;

    private final RuntimeValue<OraclePool> oraclePool;

    public OraclePoolBuildItem(String dataSourceName, RuntimeValue<OraclePool> oraclePool) {
        this.dataSourceName = dataSourceName;
        this.oraclePool = oraclePool;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public RuntimeValue<OraclePool> getOraclePool() {
        return oraclePool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }

}
