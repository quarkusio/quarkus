package io.quarkus.reactive.db2.client.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.vertx.db2client.DB2Pool;

public class DB2PoolProducer {

    @Inject
    DB2Pool db2Pool;

    /**
     * @return the <em>mutiny</em> DB2 Pool instance. The instance is created lazily.
     */
    @Singleton
    @Produces
    public io.vertx.mutiny.db2client.DB2Pool mutinyDB2Pool() {
        return io.vertx.mutiny.db2client.DB2Pool.newInstance(db2Pool);
    }
}
