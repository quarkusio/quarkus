package io.quarkus.reactive.mysql.client.deployment;

import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.vertx.mysqlclient.MySQLPool;

@Deprecated(since = "3.21", forRemoval = true)
public final class MySQLPoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;

    private final Function<SyntheticCreationalContext<MySQLPool>, MySQLPool> mysqlPool;

    public MySQLPoolBuildItem(String dataSourceName, Function<SyntheticCreationalContext<MySQLPool>, MySQLPool> mysqlPool) {
        this.dataSourceName = dataSourceName;
        this.mysqlPool = mysqlPool;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public Function<SyntheticCreationalContext<MySQLPool>, MySQLPool> getMySQLPool() {
        return mysqlPool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }

}
