package io.quarkus.reactive.pg.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.pgclient.PgPool;

public final class PgPoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;

    private final RuntimeValue<PgPool> pgPool;

    public PgPoolBuildItem(String dataSourceName, RuntimeValue<PgPool> pgPool) {
        this.dataSourceName = dataSourceName;
        this.pgPool = pgPool;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public RuntimeValue<PgPool> getPgPool() {
        return pgPool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }
}
