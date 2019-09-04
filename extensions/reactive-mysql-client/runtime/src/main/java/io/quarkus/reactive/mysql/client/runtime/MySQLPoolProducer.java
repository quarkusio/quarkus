package io.quarkus.reactive.mysql.client.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.mysqlclient.MySQLPool;

@ApplicationScoped
public class MySQLPoolProducer {

    private volatile MySQLPool mysqlPool;
    private volatile io.vertx.axle.mysqlclient.MySQLPool axleMySQLPool;
    private volatile io.vertx.reactivex.mysqlclient.MySQLPool rxMySQLPool;

    void initialize(MySQLPool mysqlPool) {
        this.mysqlPool = mysqlPool;
        this.axleMySQLPool = io.vertx.axle.mysqlclient.MySQLPool.newInstance(mysqlPool);
        this.rxMySQLPool = io.vertx.reactivex.mysqlclient.MySQLPool.newInstance(mysqlPool);
    }

    @Singleton
    @Produces
    public MySQLPool mysqlPool() {
        return mysqlPool;
    }

    @Singleton
    @Produces
    public io.vertx.axle.mysqlclient.MySQLPool axleMySQLPool() {
        return axleMySQLPool;
    }

    @Singleton
    @Produces
    public io.vertx.reactivex.mysqlclient.MySQLPool rxMySQLPool() {
        return rxMySQLPool;
    }
}
