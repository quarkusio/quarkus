package io.quarkus.reactive.pg.client.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.vertx.pgclient.PgPool;

public class PgPoolProducer {

    private static final Logger LOGGER = Logger.getLogger(PgPoolProducer.class);

    @Inject
    PgPool pgPool;

    /**
     * @return the <em>mutiny</em> PostGreSQL Pool instance. The instance is created lazily.
     */
    @Singleton
    @Produces
    public io.vertx.mutiny.pgclient.PgPool mutinyPgPool() {
        return io.vertx.mutiny.pgclient.PgPool.newInstance(pgPool);
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
    public io.vertx.axle.pgclient.PgPool axlePgPool() {
        LOGGER.warn(
                "`io.vertx.axle.pgclient.PgPool` is deprecated and will be removed in a future version - it is "
                        + "recommended to switch to `io.vertx.mutiny.pgclient.PgPool`");
        return io.vertx.axle.pgclient.PgPool.newInstance(pgPool);
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
    public io.vertx.reactivex.pgclient.PgPool rxPgPool() {
        LOGGER.warn(
                "`io.vertx.reactivex.pgclient.PgPool` is deprecated and will be removed in a future version - it is "
                        + "recommended to switch to `io.vertx.mutiny.pgclient.PgPool`");
        return io.vertx.reactivex.pgclient.PgPool.newInstance(pgPool);
    }
}
