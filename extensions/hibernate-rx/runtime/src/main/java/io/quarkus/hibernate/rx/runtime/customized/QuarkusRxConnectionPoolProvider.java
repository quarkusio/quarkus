package io.quarkus.hibernate.rx.runtime.customized;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.rx.impl.PoolConnection;
import org.hibernate.rx.service.RxConnection;
import org.hibernate.rx.service.initiator.RxConnectionPoolProvider;
import org.hibernate.service.spi.Configurable;

import io.vertx.sqlclient.Pool;

/**
 * TODO: This probably belongs in HibernateRX
 * A pool of reactive connections backed by a
 * Vert.x {@link PgPool} or {@link MySQLPool}.
 */
@SuppressWarnings("serial")
public class QuarkusRxConnectionPoolProvider implements RxConnectionPoolProvider, Configurable {

    private final Pool pool;
    private boolean showSQL;

    public QuarkusRxConnectionPoolProvider(Pool pool) {
        System.out.println("@AGG creating QuarkusRxConnectionPoolProvider with pool=" + pool);
        this.pool = pool;
    }

    @Override
    public void configure(Map configurationValues) {
        showSQL = "true".equals(configurationValues.get(AvailableSettings.SHOW_SQL));
    }

    @Override
    public RxConnection getConnection() {
        System.out.println("@AGG getting rx connection");
        return new PoolConnection(pool, showSQL);
    }

    @Override
    public void close() {
        // no-op: pool will be closed in a shutdown task
    }

}