package io.quarkus.reactive.db2.client.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.db2client.DB2Pool;

public final class DB2PoolBuildItem extends SimpleBuildItem {

    private final RuntimeValue<DB2Pool> db2Pool;

    public DB2PoolBuildItem(RuntimeValue<DB2Pool> db2Pool) {
        this.db2Pool = db2Pool;
    }

    public RuntimeValue<DB2Pool> getDB2Pool() {
        return db2Pool;
    }

}
