package io.quarkus.reactive.mysql.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.mysqlclient.MySQLPool;

public final class MySQLPoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;

    private final RuntimeValue<MySQLPool> mysqlPool;

    public MySQLPoolBuildItem(String dataSourceName, RuntimeValue<MySQLPool> mysqlPool) {
        this.dataSourceName = dataSourceName;
        this.mysqlPool = mysqlPool;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public RuntimeValue<MySQLPool> getMySQLPool() {
        return mysqlPool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }

}
