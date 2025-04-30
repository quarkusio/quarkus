package io.quarkus.reactive.db2.client.deployment;

import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.vertx.db2client.DB2Pool;

@Deprecated(since = "3.21", forRemoval = true)
public final class DB2PoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;

    private final Function<SyntheticCreationalContext<DB2Pool>, DB2Pool> db2Pool;

    public DB2PoolBuildItem(String dataSourceName, Function<SyntheticCreationalContext<DB2Pool>, DB2Pool> db2Pool) {
        this.dataSourceName = dataSourceName;
        this.db2Pool = db2Pool;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public Function<SyntheticCreationalContext<DB2Pool>, DB2Pool> getDB2Pool() {
        return db2Pool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }

}
