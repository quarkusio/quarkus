package io.quarkus.reactive.db2.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.db2client.DB2Pool;

public final class DB2PoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;

    private final RuntimeValue<DB2Pool> db2Pool;

    public DB2PoolBuildItem(String dataSourceName, RuntimeValue<DB2Pool> db2Pool) {
        this.dataSourceName = dataSourceName;
        this.db2Pool = db2Pool;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public RuntimeValue<DB2Pool> getDB2Pool() {
        return db2Pool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }

}
