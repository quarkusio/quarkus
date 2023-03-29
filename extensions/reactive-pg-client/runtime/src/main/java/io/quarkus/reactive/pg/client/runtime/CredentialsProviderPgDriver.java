package io.quarkus.reactive.pg.client.runtime;

import java.util.Map;

import io.quarkus.credentials.CredentialsProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.SocketAddress;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.impl.PgConnectionFactory;
import io.vertx.pgclient.spi.PgDriver;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.impl.Connection;
import io.vertx.sqlclient.spi.ConnectionFactory;

public class CredentialsProviderPgDriver extends PgDriver {

    public class Factory extends PgConnectionFactory {

        public Factory(VertxInternal context, PgConnectOptions options) {
            super(context, options);
        }

        @Override
        protected Future<Connection> doConnectInternal(SocketAddress server, String username, String password, String database,
                EventLoopContext context) {
            return context.executeBlocking((Promise<Map<String, String>> future) -> {
                future.complete(credentialsProvider.getCredentials(credentialsProviderName));
            })
                    .flatMap((credentials) -> {
                        String credentialsUsername = credentials.get(CredentialsProvider.USER_PROPERTY_NAME);
                        String credentialsPassword = credentials.get(CredentialsProvider.PASSWORD_PROPERTY_NAME);
                        return super.doConnectInternal(server, credentialsUsername, credentialsPassword, database, context);
                    });
        }
    }

    final private CredentialsProvider credentialsProvider;
    final private String credentialsProviderName;

    public CredentialsProviderPgDriver(CredentialsProvider credentialsProvider, String credentialsProviderName) {
        this.credentialsProvider = credentialsProvider;
        this.credentialsProviderName = credentialsProviderName;
    }

    @Override
    public ConnectionFactory createConnectionFactory(Vertx vertx, SqlConnectOptions database) {
        return new Factory((VertxInternal) vertx, PgConnectOptions.wrap(database));
    }
}
