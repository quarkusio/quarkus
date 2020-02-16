package io.quarkus.reactive.mysql.client.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.vertx.mysqlclient.MySQLPool;

@ApplicationScoped
public class MySQLPoolProducer {

    private static final Logger LOGGER = Logger.getLogger(MySQLPoolProducer.class);

    private volatile MySQLPool mysqlPool;
    private io.vertx.mutiny.mysqlclient.MySQLPool mutinyMySQLPool;

    /**
     * @deprecated The Axle API is deprecated and will be removed in the future, use {@link #mutinyMySQLPool} instead.
     */
    @Deprecated
    private io.vertx.axle.mysqlclient.MySQLPool axleMySQLPool;

    /**
     * @deprecated The RX API is deprecated and will be removed in the future, use {@link #mutinyMySQLPool} instead.
     */
    @Deprecated
    private io.vertx.reactivex.mysqlclient.MySQLPool rxMySQLPool;

    void initialize(MySQLPool mysqlPool) {
        this.mysqlPool = mysqlPool;
    }

    /**
     * @return the <em>bare</em> MySQL Pool instance.
     */
    @Singleton
    @Produces
    public MySQLPool mysqlPool() {
        return mysqlPool;
    }

    /**
     * @return the <em>mutiny</em> MySQL Pool instance. The instance is created lazily.
     */
    @Singleton
    @Produces
    public synchronized io.vertx.mutiny.mysqlclient.MySQLPool mutinyMySQLPool() {
        if (mutinyMySQLPool == null) {
            mutinyMySQLPool = io.vertx.mutiny.mysqlclient.MySQLPool.newInstance(mysqlPool);
        }
        return mutinyMySQLPool;
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
    public synchronized io.vertx.axle.mysqlclient.MySQLPool axleMySQLPool() {
        if (axleMySQLPool == null) {
            LOGGER.warn(
                    "`io.vertx.axle.mysqlclient.MySQLPool` is deprecated and will be removed in a future version - it is "
                            + "recommended to switch to `io.vertx.mutiny.mysqlclient.MySQLPool`");
            axleMySQLPool = io.vertx.axle.mysqlclient.MySQLPool.newInstance(mysqlPool);
        }
        return axleMySQLPool;
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
    public synchronized io.vertx.reactivex.mysqlclient.MySQLPool rxMySQLPool() {
        if (rxMySQLPool == null) {
            LOGGER.warn(
                    "`io.vertx.reactivex.mysqlclient.MySQLPool` is deprecated and will be removed in a future version - it is "
                            + "recommended to switch to `io.vertx.mutiny.mysqlclient.MySQLPool`");
            rxMySQLPool = io.vertx.reactivex.mysqlclient.MySQLPool.newInstance(mysqlPool);
        }
        return rxMySQLPool;
    }
}
