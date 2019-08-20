package io.quarkus.reactive.pg.client.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.pgclient.PgPool;

public final class PgPoolBuildItem extends SimpleBuildItem {

    private final RuntimeValue<PgPool> pgPool;

    public PgPoolBuildItem(RuntimeValue<PgPool> pgPool) {
        this.pgPool = pgPool;
    }

    public RuntimeValue<PgPool> getPgPool() {
        return pgPool;
    }

}
