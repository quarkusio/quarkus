package io.quarkus.reactive.mysql.client.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.mysqlclient.MySQLPool;

public final class MySQLPoolBuildItem extends SimpleBuildItem {

    private final RuntimeValue<MySQLPool> mysqlPool;

    public MySQLPoolBuildItem(RuntimeValue<MySQLPool> mysqlPool) {
        this.mysqlPool = mysqlPool;
    }

    public RuntimeValue<MySQLPool> getMySQLPool() {
        return mysqlPool;
    }

}
