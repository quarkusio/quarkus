package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.reactive.impl.PoolConnection;
import org.hibernate.reactive.service.ReactiveConnection;
import org.hibernate.reactive.service.initiator.ReactiveConnectionPoolProvider;
import org.hibernate.service.spi.Configurable;

import io.vertx.sqlclient.Pool;

/**
 * TODO: This probably belongs in HibernateRX
 * A pool of reactive connections backed by a
 * Vert.x {@link PgPool} or {@link MySQLPool}.
 */
@SuppressWarnings("serial")
public class QuarkusReactiveConnectionPoolProvider implements ReactiveConnectionPoolProvider, Configurable {

    private final Pool pool;
    private boolean showSQL;

    public QuarkusReactiveConnectionPoolProvider(Pool pool) {
        this.pool = pool;
    }

    @Override
    public void configure(Map configurationValues) {
        showSQL = "true".equals(configurationValues.get(AvailableSettings.SHOW_SQL));
    }

    @Override
    public ReactiveConnection getConnection() {
        return new PoolConnection(pool, showSQL);
    }

    @Override
    public void close() {
        // no-op: pool will be closed in a shutdown task
    }

}