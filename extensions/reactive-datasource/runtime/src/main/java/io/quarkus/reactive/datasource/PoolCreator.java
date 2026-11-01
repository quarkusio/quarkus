package io.quarkus.reactive.datasource;

import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClientOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

/**
 * This interface is an integration point that allows users to use the {@link Vertx}, {@link PoolOptions} and
 * {@link SqlConnectOptions} objects configured automatically by Quarkus, in addition to a custom strategy
 * for creating the final {@link Pool}.
 * <p>
 * Implementations of this class are meant to be used as CDI beans.
 * If a bean of this type is used without a {@link ReactiveDataSource} qualifier, then it's applied to the default datasource,
 * otherwise it applies to the datasource matching the name of the annotation.
 */
public interface PoolCreator {

    Pool create(Input input);

    interface Input {

        Vertx vertx();

        PoolOptions poolOptions();

        List<SqlConnectOptions> connectOptionsList();

        /**
         * The TCP options configured with the {@code tcp-keep-alive*} and {@code socket-*-idle-timeout} properties.
         * <p>
         * Pass them to the client builder ({@code with(NetClientOptions)}) so that the created pool honours these
         * properties.
         */
        default NetClientOptions netClientOptions() {
            return new NetClientOptions();
        }
    }
}
