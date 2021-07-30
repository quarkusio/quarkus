package io.quarkus.reactive.mssql.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.mssqlclient.MSSQLPool;

public final class MSSQLPoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;

    private final RuntimeValue<MSSQLPool> mssqlPool;

    public MSSQLPoolBuildItem(String dataSourceName, RuntimeValue<MSSQLPool> mssqlPool) {
        this.dataSourceName = dataSourceName;
        this.mssqlPool = mssqlPool;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public RuntimeValue<MSSQLPool> getMSSQLPool() {
        return mssqlPool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }

}
