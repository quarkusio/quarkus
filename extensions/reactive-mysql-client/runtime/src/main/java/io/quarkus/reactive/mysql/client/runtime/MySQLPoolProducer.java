package io.quarkus.reactive.mysql.client.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.vertx.mysqlclient.MySQLPool;

public class MySQLPoolProducer {

    private static final Logger LOGGER = Logger.getLogger(MySQLPoolProducer.class);

    @Inject
    MySQLPool mysqlPool;

    /**
     * @return the <em>mutiny</em> MySQL Pool instance. The instance is created lazily.
     */
    @Singleton
    @Produces
    public io.vertx.mutiny.mysqlclient.MySQLPool mutinyMySQLPool() {
        return io.vertx.mutiny.mysqlclient.MySQLPool.newInstance(mysqlPool);
    }

    /**
     * Produces the Axle MySQL Pool instance. The instance is created lazily.
     *
     * @return the Axle MySQL pool instance
     * @deprecated The Axle API is deprecated and will be removed in the future, use {@link #mutinyMySQLPool()} instead.
     */
    @Singleton
    @Produces
    @Deprecated
    public io.vertx.axle.mysqlclient.MySQLPool axleMySQLPool() {
        LOGGER.warn(
                "`io.vertx.axle.mysqlclient.MySQLPool` is deprecated and will be removed in a future version - it is "
                        + "recommended to switch to `io.vertx.mutiny.mysqlclient.MySQLPool`");
        return io.vertx.axle.mysqlclient.MySQLPool.newInstance(mysqlPool);
    }

    /**
     * Produces the RX MySQL Pool instance. The instance is created lazily.
     *
     * @return the RX MySQL pool instance
     * @deprecated The RX API is deprecated and will be removed in the future, use {@link #mutinyMySQLPool()} instead.
     */
    @Singleton
    @Produces
    @Deprecated
    public io.vertx.reactivex.mysqlclient.MySQLPool rxMySQLPool() {
        LOGGER.warn(
                "`io.vertx.reactivex.mysqlclient.MySQLPool` is deprecated and will be removed in a future version - it is "
                        + "recommended to switch to `io.vertx.mutiny.mysqlclient.MySQLPool`");
        return io.vertx.reactivex.mysqlclient.MySQLPool.newInstance(mysqlPool);
    }
}
