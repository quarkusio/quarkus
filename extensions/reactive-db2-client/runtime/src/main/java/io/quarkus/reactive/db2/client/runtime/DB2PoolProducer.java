package io.quarkus.reactive.db2.client.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.db2client.DB2Pool;

@ApplicationScoped
public class DB2PoolProducer {

    private volatile DB2Pool db2Pool;
    private io.vertx.mutiny.db2client.DB2Pool mutinyDB2Pool;

    void initialize(DB2Pool db2Pool) {
        this.db2Pool = db2Pool;
    }

    /**
     * @return the <em>bare</em> PostGreSQL Pool instance.
     */
    @Singleton
    @Produces
    public DB2Pool db2Pool() {
        return db2Pool;
    }

    /**
     * @return the <em>mutiny</em> DB2 Pool instance. The instance is created lazily.
     */
    @Singleton
    @Produces
    public synchronized io.vertx.mutiny.db2client.DB2Pool mutinyDB2Pool() {
        if (mutinyDB2Pool == null) {
            mutinyDB2Pool = io.vertx.mutiny.db2client.DB2Pool.newInstance(db2Pool);
        }
        return mutinyDB2Pool;
    }
}
