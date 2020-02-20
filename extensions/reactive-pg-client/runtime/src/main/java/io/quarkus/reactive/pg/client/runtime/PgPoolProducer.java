package io.quarkus.reactive.pg.client.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.vertx.pgclient.PgPool;

@ApplicationScoped
public class PgPoolProducer {

    private static final Logger LOGGER = Logger.getLogger(PgPoolProducer.class);

    private volatile PgPool pgPool;
    private io.vertx.mutiny.pgclient.PgPool mutinyPgPool;

    /**
     * @deprecated The Axle API is deprecated and will be removed in the future, use {@link #mutinyPgPool} instead.
     */
    @Deprecated
    private io.vertx.axle.pgclient.PgPool axlePgPool;
    /**
     * @deprecated The RX API is deprecated and will be removed in the future, use {@link #mutinyPgPool} instead.
     */
    @Deprecated
    private io.vertx.reactivex.pgclient.PgPool rxPgPool;

    void initialize(PgPool pgPool) {
        this.pgPool = pgPool;
    }

    /**
     * @return the <em>bare</em> PostGreSQL Pool instance.
     */
    @Singleton
    @Produces
    public PgPool pgPool() {
        return pgPool;
    }

    /**
     * @return the <em>mutiny</em> PostGreSQL Pool instance. The instance is created lazily.
     */
    @Singleton
    @Produces
    public synchronized io.vertx.mutiny.pgclient.PgPool mutinyPgPool() {
        if (mutinyPgPool == null) {
            mutinyPgPool = io.vertx.mutiny.pgclient.PgPool.newInstance(pgPool);
        }
        return mutinyPgPool;
    }

    /**
     * Produces the Axle PostGreSQL Pool instance. The instance is created lazily.
     *
     * @return the Axle PostGreSQL pool instance
     * @deprecated The Axle API is deprecated and will be removed in the future, use {@link #mutinyPgPool()} instead.
     */
    @Singleton
    @Produces
    @Deprecated
    public synchronized io.vertx.axle.pgclient.PgPool axlePgPool() {
        if (axlePgPool == null) {
            LOGGER.warn(
                    "` io.vertx.axle.pgclient.PgPool` is deprecated and will be removed in a future version - it is "
                            + "recommended to switch to `io.vertx.mutiny.pgclient.PgPool`");
            axlePgPool = io.vertx.axle.pgclient.PgPool.newInstance(pgPool);
        }
        return axlePgPool;
    }

    /**
     * Produces the RX PostGreSQL Pool instance. The instance is created lazily.
     *
     * @return the RX PostGreSQL pool instance
     * @deprecated The RX API is deprecated and will be removed in the future, use {@link #mutinyPgPool()} instead.
     */
    @Singleton
    @Produces
    @Deprecated
    public synchronized io.vertx.reactivex.pgclient.PgPool rxPgPool() {
        if (rxPgPool == null) {
            LOGGER.warn(
                    "` io.vertx.reactivex.pgclient.PgPool` is deprecated and will be removed in a future version - it is "
                            + "recommended to switch to `io.vertx.mutiny.pgclient.PgPool`");
            rxPgPool = io.vertx.reactivex.pgclient.PgPool.newInstance(pgPool);
        }
        return rxPgPool;
    }
}
