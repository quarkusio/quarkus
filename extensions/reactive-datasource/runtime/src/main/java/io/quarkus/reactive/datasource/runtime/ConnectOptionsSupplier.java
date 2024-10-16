package io.quarkus.reactive.datasource.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.quarkus.credentials.CredentialsProvider;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlConnectOptions;

public class ConnectOptionsSupplier<CO extends SqlConnectOptions> implements Supplier<Future<CO>> {

    private final Vertx vertx;
    private final CredentialsProvider credentialsProvider;
    private final String credentialsProviderName;
    private final List<CO> connectOptionsList;
    private final UnaryOperator<CO> connectOptionsCopy;
    private final Callable<CO> blockingCodeHandler;

    public ConnectOptionsSupplier(Vertx vertx, CredentialsProvider credentialsProvider, String credentialsProviderName,
            List<CO> connectOptionsList, UnaryOperator<CO> connectOptionsCopy) {
        this.vertx = vertx;
        this.credentialsProvider = credentialsProvider;
        this.credentialsProviderName = credentialsProviderName;
        this.connectOptionsList = connectOptionsList;
        this.connectOptionsCopy = connectOptionsCopy;
        this.blockingCodeHandler = new BlockingCodeHandler();
    }

    @Override
    public Future<CO> get() {
        return vertx.executeBlocking(blockingCodeHandler, false);
    }

    private class BlockingCodeHandler implements Callable<CO>, IntUnaryOperator {

        final AtomicInteger idx = new AtomicInteger();

        @Override
        public CO call() {
            Map<String, String> credentials = credentialsProvider.getCredentials(credentialsProviderName);
            String user = credentials.get(USER_PROPERTY_NAME);
            String password = credentials.get(PASSWORD_PROPERTY_NAME);

            int nextIdx = idx.getAndUpdate(this);

            CO connectOptions = connectOptionsCopy.apply(connectOptionsList.get(nextIdx));
            connectOptions.setUser(user).setPassword(password);

            return connectOptions;
        }

        @Override
        public int applyAsInt(int previousIdx) {
            return previousIdx == connectOptionsList.size() - 1 ? 0 : previousIdx + 1;
        }
    }
}
