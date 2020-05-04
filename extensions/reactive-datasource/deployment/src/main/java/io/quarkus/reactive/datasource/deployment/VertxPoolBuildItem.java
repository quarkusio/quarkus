package io.quarkus.reactive.datasource.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.sqlclient.Pool;

/**
 * A build item for Vertx {@link Pool}s.
 * <p>
 * If you inject this build item when recording runtime init template calls, you are guaranteed the Pool configuration
 * has been injected and Pools can be created.
 */
public final class VertxPoolBuildItem extends MultiBuildItem {

    private final RuntimeValue<? extends Pool> vertxPool;

    public VertxPoolBuildItem(RuntimeValue<? extends Pool> pgPool) {
        this.vertxPool = pgPool;
    }

    public RuntimeValue<? extends Pool> getPool() {
        return vertxPool;
    }

}
