package io.quarkus.reactive.pg.client.deployment;

import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.vertx.pgclient.PgPool;

@Deprecated(since = "3.21", forRemoval = true)
public final class PgPoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;

    private final Function<SyntheticCreationalContext<PgPool>, PgPool> pgPool;

    public PgPoolBuildItem(String dataSourceName, Function<SyntheticCreationalContext<PgPool>, PgPool> pgPool) {
        this.dataSourceName = dataSourceName;
        this.pgPool = pgPool;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public Function<SyntheticCreationalContext<PgPool>, PgPool> getPgPool() {
        return pgPool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }
}
