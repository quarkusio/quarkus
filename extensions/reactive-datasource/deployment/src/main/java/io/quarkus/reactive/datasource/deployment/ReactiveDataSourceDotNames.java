package io.quarkus.reactive.datasource.deployment;

import jakarta.enterprise.inject.Instance;

import org.jboss.jandex.DotName;

import io.vertx.sqlclient.Pool;

public class ReactiveDataSourceDotNames {

    public static final DotName VERTX_POOL = DotName.createSimple(Pool.class);
    public static final DotName INJECT_INSTANCE = DotName.createSimple(Instance.class);

    private ReactiveDataSourceDotNames() {
        //Utility
    }
}
