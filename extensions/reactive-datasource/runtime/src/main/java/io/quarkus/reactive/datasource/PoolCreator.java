package io.quarkus.reactive.datasource;

import java.util.List;
import java.util.function.Supplier;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
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

        /**
         * Returns the static list of connect options configured at startup.
         * <p>
         * Use this when you only need to inspect or copy the base connection settings and
         * credentials rotation is not required.
         * <p>
         * When a {@code credentials-provider} is configured or multiple hosts are listed for
         * round-robin, prefer {@link #dynamicConnectOptionsList()} so that each new connection
         * receives fresh credentials and the host selection rotates correctly.
         *
         * @return the list of connect options; never {@code null}
         */
        List<SqlConnectOptions> connectOptionsList();

        /**
         * Returns a supplier that produces the connect options for the next connection,
         * applying credential rotation and round-robin host selection.
         * <p>
         * When a {@code credentials-provider} is configured, the supplier calls the provider
         * on every invocation so that each new connection receives up-to-date credentials.
         * When multiple hosts are configured, the supplier rotates across them in round-robin order.
         * <p>
         * Custom pool creators that build the pool programmatically should pass this supplier
         * to the builder's {@code connectingTo} method:
         *
         * <pre>{@code
         * return PgBuilder.pool()
         *         .with(input.poolOptions())
         *         .connectingTo(input.dynamicConnectOptionsList())
         *         .using(input.vertx())
         *         .build();
         * }</pre>
         *
         * @return the dynamic connect-options supplier; never {@code null}
         */
        Supplier<Future<SqlConnectOptions>> dynamicConnectOptionsList();
    }
}
